package com.smartwardrobe.data.local.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 衣物单品表。
 *
 * primaryColor 采用 "颜色名称 (#RRGGBB)" 的组合格式存储，
 * 例如 "米白色 (#F5F5DC)"，同时满足「名称 + Hex 值」的要求，[colorHex] 提供快速取值。
 * 列名使用 Kotlin 属性名（Room 默认行为），与 DAO 中的 SQL 保持一致。
 */
@Entity(tableName = "clothing_items")
data class ClothingItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val imagePath: String,

    val name: String,

    val category: Category,

    val subCategory: String,

    val primaryColor: String,

    val secondaryColor: String? = null,

    val seasons: List<Season> = emptyList(),

    val styles: List<String> = emptyList(),

    val material: String = "",

    val warmthLevel: Int = 3,

    val wearCount: Int = 0,

    val lastWornDate: Long? = null,

    val createdAt: Long = System.currentTimeMillis()
) {
    /** 从 primaryColor 中解析出的 Hex 值，供色卡筛选与 UI 着色使用。 */
    @Ignore
    val colorHex: String = HEX_REGEX.find(primaryColor)?.value.orEmpty()

    /** 从 primaryColor 中解析出的中文颜色名称。 */
    @Ignore
    val colorName: String = primaryColor.substringBefore("(").trim().ifEmpty { primaryColor }

    companion object {
        private val HEX_REGEX = Regex("#[0-9A-Fa-f]{6}")

        /** 由 AI 打标的名称与 Hex 组装为标准主色字符串。 */
        fun buildPrimaryColor(name: String, hex: String): String {
            val normalized = if (hex.startsWith("#")) hex else "#$hex"
            return "$name ($normalized)".trim()
        }
    }
}
