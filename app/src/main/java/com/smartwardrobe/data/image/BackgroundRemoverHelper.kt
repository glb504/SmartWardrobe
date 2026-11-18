package com.smartwardrobe.data.image

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 端侧自动抠图：封装 ML Kit Subject Segmentation。
 * 输入包含杂乱背景的衣物照片，输出透明背景 Bitmap，并由 [ImageFileManager] 持久化为 PNG。
 * 全流程在设备端完成，无需联网。
 */
@Singleton
class BackgroundRemoverHelper @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val fileManager: ImageFileManager
) {
    private val segmenter by lazy {
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()
        SubjectSegmentation.getClient(options)
    }

    /** 抠除背景并保存，返回透明 PNG 绝对路径。 */
    suspend fun removeBackgroundAndSave(source: Bitmap, prefix: String = "item"): String =
        withContext(Dispatchers.Default) {
            val transparent = removeBackground(source)
            fileManager.saveTransparentPng(transparent, prefix)
        }

    /** 抠除背景，返回透明背景 Bitmap。 */
    suspend fun removeBackground(source: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val input = InputImage.fromBitmap(source, 0)
        val result = segmenter.process(input).await()
        // getForegroundBitmap 返回已按主体 mask 裁剪、背景透明的前景图
        result.getForegroundBitmap() ?: source
    }
}
