package com.smartwardrobe.data.image

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 真人形象提取：封装 ML Kit Selfie Segmentation。
 * 用于虚拟试衣间 —— 从用户自拍中提取人像主体（透明背景），作为衣物图层叠加的底图。
 * 静态内置模型，完全离线；SINGLE_IMAGE_MODE 适合单张照片精确分割。
 */
@Singleton
class PortraitSegmentationHelper @Inject constructor(
    private val fileManager: ImageFileManager
) {
    private val segmenter by lazy {
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .build()
        Segmentation.getClient(options)
    }

    /** 提取人像并保存为透明 PNG，返回绝对路径。 */
    suspend fun extractPortraitAndSave(source: Bitmap): String = withContext(Dispatchers.Default) {
        val transparent = extractPortrait(source)
        fileManager.savePortraitPng(transparent)
    }

    /** 提取人像，返回透明背景 Bitmap。 */
    suspend fun extractPortrait(source: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val input = InputImage.fromBitmap(source, 0)
        val mask = segmenter.process(input).await()
        // selfie mask 的 buffer 每个像素是一个 Float(0..1) 前景置信度
        BitmapMaskUtil.applyFloatMask(source, mask.buffer, threshold = 0.5f)
    }
}
