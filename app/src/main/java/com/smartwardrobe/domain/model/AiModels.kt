package com.smartwardrobe.domain.model

import com.smartwardrobe.data.local.entity.Category
import com.smartwardrobe.data.local.entity.Season

/** 视觉打标 Agent 的结构化输出（已做容错解析后的领域模型）。 */
data class ClothingAnalysis(
    val name: String,
    val category: Category,
    val subCategory: String,
    val primaryColor: String,
    val primaryColorHex: String,
    val secondaryColor: String? = null,
    val seasons: List<Season> = listOf(Season.SPRING, Season.SUMMER, Season.AUTUMN, Season.WINTER),
    val styles: List<String> = emptyList(),
    val material: String = "",
    val warmthLevel: Int = 3
)

/** 穿搭场景上下文：由用户输入 + 系统检索共同构成。 */
data class StylistContext(
    val occasion: String,          // 通勤 / 约会 / 运动 / 旅行 / 聚会 / 日常
    val temperatureC: Int,         // 当前温度（摄氏度）
    val weather: String,           // 晴 / 多云 / 雨 / 雪 / 风
    val season: Season,
    val moodOrRequest: String = "", // 今日心情或特殊要求
    val bodyType: String = "",     // 用户体型特征
    val skinTone: String = ""      // 用户肤色特征
)

/** 单套 AI 穿搭建议（尚未落库，ID 为衣橱中真实存在的单品 ID）。 */
data class StylistSuggestion(
    val title: String,
    val selectedItemIds: List<String>,
    val aiReasoning: String,
    val stylingTips: String = "",
    val targetTempRange: String = ""
)
