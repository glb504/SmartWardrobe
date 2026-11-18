package com.smartwardrobe.data.local.converter

import androidx.room.TypeConverter
import com.smartwardrobe.data.local.entity.Category
import com.smartwardrobe.data.local.entity.Season
import com.smartwardrobe.data.local.entity.WearTargetType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room 类型转换器。
 * 集合与枚举统一序列化为 JSON 字符串存储，保证类型安全且可读。
 */
class Converters {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // ---------- List<Season> ----------
    @TypeConverter
    fun fromSeasonList(value: List<Season>): String = json.encodeToString(value)

    @TypeConverter
    fun toSeasonList(value: String?): List<Season> =
        if (value.isNullOrBlank()) emptyList()
        else runCatching { json.decodeFromString<List<Season>>(value) }.getOrDefault(emptyList())

    // ---------- List<String> ----------
    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrBlank()) emptyList()
        else runCatching { json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())

    // ---------- Category ----------
    @TypeConverter
    fun fromCategory(value: Category): String = value.name

    @TypeConverter
    fun toCategory(value: String?): Category =
        value?.let { Category.fromName(it) } ?: Category.TOP_INNER

    // ---------- WearTargetType ----------
    @TypeConverter
    fun fromWearTargetType(value: WearTargetType): String = value.name

    @TypeConverter
    fun toWearTargetType(value: String?): WearTargetType =
        value?.let { raw -> WearTargetType.entries.firstOrNull { it.name == raw.trim().uppercase() } }
            ?: WearTargetType.ITEM
}
