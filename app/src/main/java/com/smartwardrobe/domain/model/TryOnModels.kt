package com.smartwardrobe.domain.model

import com.smartwardrobe.data.local.entity.Category

/**
 * 虚拟试衣间领域模型。
 *
 * 试衣采用「图层合成」方案：以抠出的真人透明 PNG 为底图，
 * 将衣橱中的透明衣物 PNG 按部位叠加，通过变换参数（缩放/位移/旋转/层级）对齐到人体。
 * 支持一键套用 AI 搭配，也支持用户逐件替换。
 */
object TryOnSlots {
    /** 由衣物品类映射到试衣图层槽位。 */
    fun fromCategory(category: Category): String = when (category) {
        Category.ONE_PIECE -> "FULL_BODY"
        Category.TOP_INNER -> "TOP_INNER"
        Category.TOP_OUTER -> "TOP_OUTER"
        Category.BOTTOM -> "BOTTOM"
        Category.SHOES -> "SHOES"
        Category.BAG -> "BAG"
        Category.ACCESSORY -> "ACCESSORY"
    }

    /** 合成顺序：数值越小越先绘制（越靠底层）。 */
    fun zIndex(slot: String): Int = when (slot) {
        "FULL_BODY" -> 1
        "BOTTOM" -> 2
        "TOP_INNER" -> 3
        "SHOES" -> 4
        "TOP_OUTER" -> 5
        "BAG" -> 6
        "ACCESSORY" -> 7
        else -> 8
    }

    /** 各槽位的默认变换：相对于人像画布宽高的比例位置。 */
    fun defaultTransform(slot: String): LayerTransform = when (slot) {
        "FULL_BODY" -> LayerTransform(scale = 0.78f, offsetX = 0f, offsetY = 0.02f)
        "TOP_INNER" -> LayerTransform(scale = 0.62f, offsetX = 0f, offsetY = -0.16f)
        "TOP_OUTER" -> LayerTransform(scale = 0.68f, offsetX = 0f, offsetY = -0.14f)
        "BOTTOM" -> LayerTransform(scale = 0.55f, offsetX = 0f, offsetY = 0.20f)
        "SHOES" -> LayerTransform(scale = 0.26f, offsetX = 0f, offsetY = 0.40f)
        "BAG" -> LayerTransform(scale = 0.28f, offsetX = 0.28f, offsetY = 0.06f)
        "ACCESSORY" -> LayerTransform(scale = 0.22f, offsetX = 0f, offsetY = -0.30f)
        else -> LayerTransform()
    }
}

/** 单个衣物图层的变换参数（比例坐标，-1..1 相对画布中心）。 */
data class LayerTransform(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f,
    val alpha: Float = 1f
)

/** 试衣图层：绑定一件真实衣物 + 变换参数。 */
data class TryOnLayer(
    val slot: String,
    val clothingItemId: String,
    val imagePath: String,
    val name: String,
    val transform: LayerTransform = TryOnSlots.defaultTransform("TOP_INNER")
) {
    val zIndex: Int get() = TryOnSlots.zIndex(slot)
}

/** 一次试衣会话：人像底图 + 当前穿着的所有图层。 */
data class TryOnSession(
    val portraitPath: String,
    val layers: List<TryOnLayer> = emptyList(),
    val outfitTitle: String = "自由搭配"
)
