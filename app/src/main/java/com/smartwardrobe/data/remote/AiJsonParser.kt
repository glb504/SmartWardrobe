package com.smartwardrobe.data.remote

import com.smartwardrobe.data.local.entity.Category
import com.smartwardrobe.data.local.entity.Season
import com.smartwardrobe.domain.model.ClothingAnalysis
import com.smartwardrobe.domain.model.StylistSuggestion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI JSON 容错解析：
 * - 剥离 ```json ... ``` 代码块与前后多余文字
 * - 提取首个完整 JSON 对象（大括号配平）
 * - 字段缺失/类型不符时使用安全默认值，绝不抛异常中断流水线
 */
@Singleton
class AiJsonParser @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** 从可能带噪声的模型输出中提取第一个 JSON 对象。 */
    fun extractJsonObject(raw: String): JsonObject? {
        var text = raw.trim()
        // 去掉 markdown 代码块围栏
        text = text.removePrefix("```json").removePrefix("```JSON").removePrefix("```")
        val endFence = text.lastIndexOf("```")
        if (endFence > 0) text = text.substring(0, endFence)
        text = text.trim()

        val start = text.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until text.length) {
            val c = text[i]
            if (inString) {
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == '"') inString = false
                continue
            }
            when (c) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        val candidate = text.substring(start, i + 1)
                        return runCatching { json.parseToJsonElement(candidate) as? JsonObject }.getOrNull()
                    }
                }
            }
        }
        return null
    }

    /** 解析视觉打标结果。 */
    fun parseClothingAnalysis(raw: String): ClothingAnalysis? {
        val obj = extractJsonObject(raw) ?: return null
        val name = obj.str("name")?.takeIf { it.isNotBlank() } ?: "未识别衣物"
        val category = Category.fromName(obj.str("category")) ?: Category.TOP_INNER
        val subCategory = obj.str("subCategory") ?: ""
        val colorName = obj.str("primaryColor") ?: "未知色"
        var hex = obj.str("primaryColorHex") ?: "#CCCCCC"
        if (!hex.startsWith("#")) hex = "#$hex"
        if (!Regex("^#[0-9A-Fa-f]{6}$").matches(hex)) hex = "#CCCCCC"

        val seasonsRaw = obj.array("seasons")
        val seasons = if (seasonsRaw.isNullOrEmpty()) Season.entries.toList()
        else seasonsRaw.mapNotNull { Season.fromName(it.asPrimitive()?.contentOrNull) }
            .ifEmpty { Season.entries.toList() }

        val styles = obj.array("styles")?.mapNotNull { it.asPrimitive()?.contentOrNull }
            ?.filter { it.isNotBlank() } ?: emptyList()

        val material = obj.str("material") ?: ""
        val warmth = obj["warmthLevel"]?.asPrimitive()?.intOrNull
            ?: obj.str("warmthLevel")?.toIntOrNull()
        val warmthLevel = (warmth ?: 3).coerceIn(1, 5)

        return ClothingAnalysis(
            name = name,
            category = category,
            subCategory = subCategory,
            primaryColor = colorName,
            primaryColorHex = hex,
            secondaryColor = obj.str("secondaryColor"),
            seasons = seasons,
            styles = styles,
            material = material,
            warmthLevel = warmthLevel
        )
    }

    /** 解析穿搭推荐结果。 */
    fun parseStylistSuggestions(raw: String): List<StylistSuggestion> {
        val obj = extractJsonObject(raw) ?: return emptyList()
        val array = obj["outfits"] as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            val o = element as? JsonObject ?: return@mapNotNull null
            val ids = o.array("selectedItemIds")?.mapNotNull { it.asPrimitive()?.contentOrNull }
                ?.filter { it.isNotBlank() } ?: return@mapNotNull null
            if (ids.isEmpty()) return@mapNotNull null
            StylistSuggestion(
                title = o.str("title")?.takeIf { it.isNotBlank() } ?: "AI 推荐搭配",
                selectedItemIds = ids,
                aiReasoning = o.str("aiReasoning") ?: "",
                stylingTips = o.str("stylingTips") ?: "",
                targetTempRange = o.str("targetTempRange") ?: ""
            )
        }
    }

    // ---- 安全取值辅助 ----
    private fun JsonObject.str(key: String): String? =
        this[key]?.asPrimitive()?.contentOrNull?.takeIf { it != "null" }

    private fun JsonObject.array(key: String): JsonArray? = this[key] as? JsonArray

    private fun JsonElement.asPrimitive(): JsonPrimitive? = this as? JsonPrimitive
}
