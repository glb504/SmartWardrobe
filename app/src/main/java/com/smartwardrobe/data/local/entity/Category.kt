package com.smartwardrobe.data.local.entity

import kotlinx.serialization.Serializable

/** 衣物一级品类，与视觉打标 Agent 输出的 category 字段一一对应。 */
@Serializable
enum class Category {
    TOP_INNER,   // 内搭（T恤、衬衫、针织衫…）
    TOP_OUTER,   // 外套（西装、夹克、大衣…）
    BOTTOM,      // 下装（裤、裙）
    ONE_PIECE,   // 连体（连衣裙、连体裤）
    SHOES,       // 鞋履
    ACCESSORY,   // 配饰（围巾、帽、腰带…）
    BAG;         // 包袋

    companion object {
        fun fromName(value: String?): Category? =
            value?.trim()?.uppercase()?.let { raw ->
                entries.firstOrNull { it.name == raw }
            }
    }
}
