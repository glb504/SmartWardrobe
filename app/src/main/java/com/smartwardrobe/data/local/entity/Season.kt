package com.smartwardrobe.data.local.entity

import kotlinx.serialization.Serializable

/** 适用季节，可多选。 */
@Serializable
enum class Season {
    SPRING, SUMMER, AUTUMN, WINTER;

    companion object {
        fun fromName(value: String?): Season? =
            value?.trim()?.uppercase()?.let { raw ->
                entries.firstOrNull { it.name == raw }
                    ?: when (raw) {
                        "春", "春季" -> SPRING
                        "夏", "夏季" -> SUMMER
                        "秋", "秋季" -> AUTUMN
                        "冬", "冬季" -> WINTER
                        else -> null
                    }
            }
    }
}
