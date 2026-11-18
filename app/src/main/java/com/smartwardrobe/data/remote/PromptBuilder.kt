package com.smartwardrobe.data.remote

import com.smartwardrobe.data.local.entity.Category
import com.smartwardrobe.data.local.entity.ClothingItemEntity
import com.smartwardrobe.domain.model.StylistContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 多模态 Prompt 构建器：
 * 1) 视觉入库打标
 * 2) 衣橱库存精简 JSON 序列化（不含图片，仅 id/name/category/primaryColor/styles/warmthLevel）
 * 3) 智能穿搭推荐
 */
@Singleton
class PromptBuilder @Inject constructor() {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    /** 视觉入库 Agent 的 System Prompt，强制返回指定 schema 的 JSON。 */
    fun buildVisionTagSystemPrompt(): String = """
Analyze this clothing item image and return ONLY a valid JSON object matching the schema:
{
  "name": string,
  "category": one of ["TOP_INNER","TOP_OUTER","BOTTOM","ONE_PIECE","SHOES","ACCESSORY","BAG"],
  "subCategory": string,
  "primaryColor": string,
  "primaryColorHex": string,
  "seasons": array of subset ["SPRING","SUMMER","AUTUMN","WINTER"],
  "styles": array of strings (e.g. 休闲,通勤,街头,复古,极简,运动),
  "material": string,
  "warmthLevel": integer 1-5
}
Rules:
- Use Simplified Chinese for all descriptive fields (name, subCategory, primaryColor, styles, material).
- category and seasons MUST use the exact uppercase enum values listed above.
- primaryColorHex MUST be a 6-digit hex color starting with #, e.g. "#F5F5DC".
- warmthLevel: 1=extremely thin summer wear, 5=heavy winter down coat.
- Output ONLY the JSON object. No markdown, no explanation, no code fences.
""".trimIndent()

    fun buildVisionTagUserPrompt(): String =
        "识别图中的衣物单品，严格按照 schema 返回 JSON。这是一张已抠除背景的透明衣物图片。"

    /** 精简版衣物清单，仅保留穿搭推荐必要字段，节省 Token 与延迟。 */
    @Serializable
    data class WardrobeLite(
        val id: String,
        val name: String,
        val category: String,
        val primaryColor: String,
        val styles: List<String>,
        val warmthLevel: Int
    )

    fun serializeWardrobe(items: List<ClothingItemEntity>): String {
        val lite = items.map {
            WardrobeLite(
                id = it.id,
                name = it.name,
                category = it.category.name,
                primaryColor = it.colorName.ifEmpty { it.primaryColor },
                styles = it.styles,
                warmthLevel = it.warmthLevel
            )
        }
        return json.encodeToString(lite)
    }

    /** 智能穿搭 Agent 的 System Prompt。 */
    fun buildStylistSystemPrompt(): String = """
You are a professional fashion stylist AI. You will be given:
- a wardrobe inventory JSON (each item has id, name, category, primaryColor, styles, warmthLevel),
- today's context (occasion, temperature, weather, season, mood/request, body type, skin tone).

Generate 2-3 DISTINCT outfit ideas with different concepts (e.g. one safe everyday look, one bold elevated look).
CRITICAL RULES:
- You MUST ONLY use item ids that exist in the wardrobe JSON. Never invent ids.
- Each outfit must be a sensible combination (usually: inner/outer top + bottom or one-piece + shoes, plus optional accessory/bag).
- Respect the temperature and warmthLevel: hotter weather => lower total warmth; colder => higher.
- Consider the user's mood/special request.
Return ONLY a valid JSON object in this exact schema:
{
  "outfits": [
    {
      "title": string,
      "selectedItemIds": [string],
      "aiReasoning": string,
      "stylingTips": string,
      "targetTempRange": string
    }
  ]
}
Use Simplified Chinese for all descriptive text. No markdown, no code fences, no explanation.
""".trimIndent()

    /** 智能穿搭 Agent 的 User Prompt（携带上下文 + 库存）。 */
    fun buildStylistUserPrompt(context: StylistContext, wardrobeJson: String): String = buildString {
        appendLine("今日上下文：")
        appendLine("- 场合：${context.occasion}")
        appendLine("- 温度：${context.temperatureC}°C")
        appendLine("- 天气：${context.weather}")
        appendLine("- 季节：${context.season.name}")
        appendLine("- 心情/特殊要求：${context.moodOrRequest.ifEmpty { "无" }}")
        appendLine("- 体型：${context.bodyType.ifEmpty { "未提供" }}")
        appendLine("- 肤色：${context.skinTone.ifEmpty { "未提供" }}")
        appendLine()
        appendLine("可用衣橱库存 JSON：")
        appendLine(wardrobeJson)
        appendLine()
        append("请返回 2-3 套穿搭方案的 JSON。")
    }

    /** 从符合季节/温度的衣物生成库存 JSON 的入口（供 Repository 调用）。 */
    fun wardrobeToJson(items: List<ClothingItemEntity>): String = serializeWardrobe(items)
}
