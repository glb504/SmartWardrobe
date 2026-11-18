package com.smartwardrobe.data.repository

import android.util.Log
import com.smartwardrobe.data.image.ImageFileManager
import com.smartwardrobe.data.remote.AiJsonParser
import com.smartwardrobe.data.remote.AiService
import com.smartwardrobe.data.remote.PromptBuilder
import com.smartwardrobe.data.remote.dto.ChatMessage
import com.smartwardrobe.data.remote.dto.ChatRequest
import com.smartwardrobe.data.remote.dto.ResponseFormat
import com.smartwardrobe.data.remote.dto.ImagePart
import com.smartwardrobe.data.remote.dto.ImageUrl
import com.smartwardrobe.data.remote.dto.TextPart
import com.smartwardrobe.domain.model.ClothingAnalysis
import com.smartwardrobe.domain.model.StylistContext
import com.smartwardrobe.domain.model.StylistSuggestion
import com.smartwardrobe.domain.repository.AiRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val service: AiService,
    private val promptBuilder: PromptBuilder,
    private val parser: AiJsonParser,
    private val imageFileManager: ImageFileManager,
    private val json: Json
) : AiRepository {

    override suspend fun analyzeClothing(imagePath: String): ClothingAnalysis {
        val bitmap = imageFileManager.decode(imagePath)
            ?: throw IllegalArgumentException("无法读取衣物图片: $imagePath")
        val scaled = imageFileManager.scaleForNetwork(bitmap)
        val dataUrl = imageFileManager.toDataUrl(saveTmp(scaled))

        // 内容块数组：文本 + 图片（DeepSeek vision 接口要求）
        val parts = listOf(
            TextPart(text = promptBuilder.buildVisionTagUserPrompt()),
            ImagePart(imageUrl = ImageUrl(url = dataUrl, detail = "low"))
        )
        val content: JsonElement = json.parseToJsonElement(json.encodeToString(parts))

        val request = ChatRequest(
            model = VISION_MODEL,
            messages = listOf(
                ChatMessage(role = "system", content = json.parseToJsonElement(json.encodeToString(promptBuilder.buildVisionTagSystemPrompt()))),
                ChatMessage(role = "user", content = content)
            ),
            responseFormat = ResponseFormat(type = "json_object"),
            maxTokens = 1024
        )
        val raw = extractContent(request)
        return parser.parseClothingAnalysis(raw)
            ?: throw IllegalStateException("AI 打标结果解析失败: ${raw.take(200)}")
    }

    override suspend fun generateOutfits(
        context: StylistContext,
        wardrobeJson: String
    ): List<StylistSuggestion> {
        val request = ChatRequest(
            model = TEXT_MODEL,
            messages = listOf(
                ChatMessage(role = "system", content = json.parseToJsonElement(json.encodeToString(promptBuilder.buildStylistSystemPrompt()))),
                ChatMessage(role = "user", content = json.parseToJsonElement(json.encodeToString(promptBuilder.buildStylistUserPrompt(context, wardrobeJson))))
            ),
            responseFormat = ResponseFormat(type = "json_object"),
            maxTokens = 2048
        )
        val raw = extractContent(request)
        val suggestions = parser.parseStylistSuggestions(raw)
        if (suggestions.isEmpty()) Log.w(TAG, "穿搭方案解析为空: ${raw.take(200)}")
        return suggestions
    }

    private suspend fun extractContent(request: ChatRequest): String {
        val response = service.chat(request)
        if (response.error != null) {
            throw RuntimeException("AI 接口错误: ${response.error.message}")
        }
        return response.choices?.firstOrNull()?.message?.content
            ?: throw IllegalStateException("AI 返回内容为空")
    }

    /** 缩放后的临时图片需落盘再编码（toDataUrl 基于路径读取）。 */
    private fun saveTmp(bitmap: android.graphics.Bitmap): String =
        imageFileManager.saveTransparentPng(bitmap, "network_tmp")

    companion object {
        private const val TAG = "AiRepository"
        private const val VISION_MODEL = "deepseek-v4-flash-vision-exp"
        private const val TEXT_MODEL = "deepseek-v4-flash"
    }
}
