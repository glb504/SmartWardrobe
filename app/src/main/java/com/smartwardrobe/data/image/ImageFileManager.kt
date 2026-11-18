package com.smartwardrobe.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 内部存储图片管理：保存抠图后的透明 PNG，并提供 Base64 编码供多模态 LLM 使用。
 * 目录：context.filesDir/wardrobe_images/
 */
@Singleton
class ImageFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val wardrobeDir: File
        get() = File(context.filesDir, DIR_WARDROBE).apply { if (!exists()) mkdirs() }

    /** 保存透明 PNG，返回绝对路径。 */
    fun saveTransparentPng(bitmap: Bitmap, prefix: String = "item"): String {
        val file = File(wardrobeDir, "${prefix}_${UUID.randomUUID()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        return file.absolutePath
    }

    /** 保存真人形象 PNG（用于虚拟试衣间底图）。 */
    fun savePortraitPng(bitmap: Bitmap): String = saveTransparentPng(bitmap, "portrait")

    fun decode(path: String): Bitmap? = try {
        BitmapFactoryCompat.decode(path)
    } catch (e: Exception) {
        Log.e(TAG, "decode failed: $path", e)
        null
    }

    fun delete(path: String): Boolean = try {
        File(path).delete()
    } catch (e: Exception) {
        Log.e(TAG, "delete failed: $path", e)
        false
    }

    /** 将本地图片编码为 LLM 可用的 data URL。 */
    fun toDataUrl(path: String): String {
        val bytes = File(path).readBytes()
        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        return "data:image/png;base64,$base64"
    }

    /** 大图等比缩放，避免 base64 过大（LLM 侧单图 384 token 上限，长边建议 <= 1024）。 */
    fun scaleForNetwork(source: Bitmap, maxDimension: Int = 1024): Bitmap {
        val w = source.width
        val h = source.height
        val longest = maxOf(w, h)
        if (longest <= maxDimension) return source
        val ratio = maxDimension.toFloat() / longest
        val nw = (w * ratio).toInt().coerceAtLeast(1)
        val nh = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, nw, nh, true)
    }

    private object BitmapFactoryCompat {
        fun decode(path: String): Bitmap = android.graphics.BitmapFactory.decodeFile(path)
            ?: throw IllegalArgumentException("cannot decode bitmap: $path")
    }

    companion object {
        private const val TAG = "ImageFileManager"
        const val DIR_WARDROBE = "wardrobe_images"
    }
}

/**
 * 通用位图工具：把分割 mask 应用到原图，生成透明背景 Bitmap。
 */
object BitmapMaskUtil {

    /**
     * @param source   原始图片
     * @param mask     分割 mask（灰度图：白=前景，黑=背景；或 selfie 浮点 mask）
     * @param threshold 前景置信度阈值 0..255
     */
    fun applyGrayMask(source: Bitmap, mask: Bitmap, threshold: Int = 128): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val src = IntArray(source.width * source.height)
        source.getPixels(src, 0, source.width, 0, 0, source.width, source.height)

        // mask 尺寸可能与 source 不同，做等比采样
        val mw = mask.width
        val mh = mask.height
        val maskPixels = IntArray(mw * mh)
        mask.getPixels(maskPixels, 0, mw, 0, 0, mw, mh)

        val outPixels = IntArray(src.size)
        for (y in 0 until source.height) {
            val my = (y * mh / source.height).coerceIn(0, mh - 1)
            for (x in 0 until source.width) {
                val mx = (x * mw / source.width).coerceIn(0, mw - 1)
                val luminance = Color.red(maskPixels[my * mw + mx])
                val index = y * source.width + x
                val color = src[index]
                val alpha = if (luminance >= threshold) (color ushr 24) else 0
                outPixels[index] = (alpha shl 24) or (color and 0x00FFFFFF)
            }
        }
        out.setPixels(outPixels, 0, source.width, 0, 0, source.width, source.height)
        return out
    }

    /**
     * 应用 selfie segmentation 的浮点置信度 mask（ByteBuffer，每像素一个 Float，4 字节，小端）。
     */
    fun applyFloatMask(source: Bitmap, buffer: java.nio.ByteBuffer, threshold: Float = 0.5f): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val src = IntArray(source.width * source.height)
        source.getPixels(src, 0, source.width, 0, 0, source.width, source.height)
        buffer.rewind()
        val floatBuffer = buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN).asFloatBuffer()

        val outPixels = IntArray(src.size)
        for (i in src.indices) {
            val confidence = if (floatBuffer.hasRemaining()) floatBuffer.get() else 0f
            val color = src[i]
            val alpha = if (confidence >= threshold) (color ushr 24) else 0
            outPixels[i] = (alpha shl 24) or (color and 0x00FFFFFF)
        }
        out.setPixels(outPixels, 0, source.width, 0, 0, source.width, source.height)
        return out
    }

    /** 裁掉透明边缘，返回紧凑 bitmap（可选，减少试衣图层占用）。 */
    fun cropTransparentEdges(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        var minX = w; var minY = h; var maxX = -1; var maxY = -1
        for (y in 0 until h) {
            for (x in 0 until w) {
                if ((pixels[y * w + x] ushr 24) > 8) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < minX || maxY < minY) return bitmap
        return Bitmap.createBitmap(bitmap, minX, minY, maxX - minX + 1, maxY - minY + 1)
    }
}
