package com.smartwardrobe.data.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 杂志拼贴 + 虚拟试衣的合成画布引擎。
 *
 * 两个职责：
 * 1) composeOutfitCollage：把搭配选中的多件透明 PNG 按杂志拼贴布局合成一张预览图（OutfitCollageCard 数据源）。
 * 2) composeTryOn：把真人透明底图与各衣物图层按 z-order + 变换参数合成试衣效果图。
 */
@Singleton
class OutfitCanvasComposer @Inject constructor(
    private val fileManager: ImageFileManager
) {

    /**
     * 虚拟试衣合成：人像底图 + 有序衣物图层 -> 一张 Bitmap。
     */
    fun composeTryOn(
        portraitPath: String,
        layers: List<com.smartwardrobe.domain.model.TryOnLayer>,
        canvasWidth: Int = 720,
        canvasHeight: Int = 1080
    ): Bitmap {
        val canvas = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val graphics = Canvas(canvas)

        // 1) 绘制人像底图：等比缩放填满画布高度，水平居中
        fileManager.decode(portraitPath)?.let { portrait ->
            val scale = canvasHeight.toFloat() / portrait.height
            val w = (portrait.width * scale).toInt()
            val left = (canvasWidth - w) / 2f
            val dst = android.graphics.RectF(left, 0f, left + w, canvasHeight.toFloat())
            graphics.drawBitmap(portrait, null, dst, null)
        }

        // 2) 按 z-index 从底到顶绘制各衣物图层
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        layers.sortedBy { it.zIndex }.forEach { layer ->
            val garment = fileManager.decode(layer.imagePath) ?: return@forEach
            val t = layer.transform
            paint.alpha = (t.alpha * 255).toInt().coerceIn(0, 255)

            // 目标尺寸：以人像宽度为基准 * scale，保持衣物纵横比
            val baseWidth = canvasWidth * 0.9f
            val targetW = baseWidth * t.scale
            val targetH = targetW * garment.height / garment.width

            val cx = canvasWidth / 2f + t.offsetX * canvasWidth
            val cy = canvasHeight / 2f + t.offsetY * canvasHeight

            graphics.save()
            graphics.translate(cx, cy)
            graphics.rotate(t.rotation)
            val dst = android.graphics.RectF(-targetW / 2f, -targetH / 2f, targetW / 2f, targetH / 2f)
            graphics.drawBitmap(garment, null, dst, paint)
            graphics.restore()
        }
        return canvas
    }

    /**
     * 杂志拼贴合成：把多件衣物透明 PNG 按 2 列网格排布，合成预览图。
     */
    fun composeOutfitCollage(
        imagePaths: List<String>,
        canvasWidth: Int = 600,
        canvasHeight: Int = 600
    ): Bitmap {
        val canvas = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val graphics = Canvas(canvas)
        graphics.drawColor(Color.TRANSPARENT)

        if (imagePaths.isEmpty()) return canvas
        val cols = if (imagePaths.size <= 2) 1 else 2
        val rows = (imagePaths.size + cols - 1) / cols
        val cellW = canvasWidth.toFloat() / cols
        val cellH = canvasHeight.toFloat() / rows
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        imagePaths.forEachIndexed { index, path ->
            val bitmap = fileManager.decode(path) ?: return@forEachIndexed
            val col = index % cols
            val row = index / cols
            val maxW = cellW * 0.88f
            val maxH = cellH * 0.88f
            val ratio = minOf(maxW / bitmap.width, maxH / bitmap.height)
            val w = bitmap.width * ratio
            val h = bitmap.height * ratio
            val cx = col * cellW + cellW / 2f
            val cy = row * cellH + cellH / 2f
            val dst = android.graphics.RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
            graphics.drawBitmap(bitmap, null, dst, paint)
        }
        return canvas
    }
}
