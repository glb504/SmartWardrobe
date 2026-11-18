package com.smartwardrobe.domain.model

import com.smartwardrobe.data.local.entity.Category
import com.smartwardrobe.data.local.entity.Season

/**
 * 衣橱多维筛选条件。所有条件之间是「与」关系；同一集合内是「或」关系。
 *
 * @param category     品类，null 表示不限
 * @param seasons      季节集合，空表示不限
 * @param styles       风格集合，空表示不限
 * @param colorFamily  色卡名称（如「蓝色」「黑色」），null/空表示不限
 * @param keyword      名称/子品类/材质的模糊搜索词
 */
data class ClothingFilter(
    val category: Category? = null,
    val seasons: Set<Season> = emptySet(),
    val styles: Set<String> = emptySet(),
    val colorFamily: String? = null,
    val keyword: String = ""
) {
    val isActive: Boolean
        get() = category != null || seasons.isNotEmpty() || styles.isNotEmpty() ||
            !colorFamily.isNullOrBlank() || keyword.isNotBlank()
}
