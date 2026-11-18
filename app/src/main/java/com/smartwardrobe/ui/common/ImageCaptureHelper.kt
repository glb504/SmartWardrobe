package com.smartwardrobe.ui.common

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** PhotoPicker 选图（Android 13+ 系统照片选择器，权限更安全）。 */
@Composable
fun rememberPickImageLauncher(onBitmap: (Bitmap) -> Unit): androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest> {
    val context = LocalContext.current
    return rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onBitmap(uriToBitmap(context, uri))
    }
}

/** 构造"仅图片"的 PhotoPicker 请求。 */
fun imageOnlyRequest(): PickVisualMediaRequest =
    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

/** 相册 Uri -> Bitmap。 */
fun uriToBitmap(context: Context, uri: Uri): Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
} else {
    @Suppress("DEPRECATION")
    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
}

/** 创建拍照临时文件。 */
fun createImageFile(context: Context): File {
    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
    val dir = File(context.cacheDir, "camera").apply { if (!exists()) mkdirs() }
    return File(dir, "IMG_$ts.jpg")
}

/**
 * CameraX 相机预览 + 拍照。
 * 点击预览区即拍照，成功后回调图片文件（调用方负责 decode）。
 */
@Composable
fun CameraXPreview(
    modifier: Modifier = Modifier,
    onPhotoTaken: (File) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize(),
        update = { view ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val provider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(view.surfaceProvider)
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    onError(e.message ?: "相机启动失败")
                }
            }, ContextCompat.getMainExecutor(context))

            view.setOnClickListener {
                val file = createImageFile(context)
                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                imageCapture.takePicture(
                    outputOptions,
                    executor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                onPhotoTaken(file)
                            }
                        }
                        override fun onError(exception: ImageCaptureException) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                onError(exception.message ?: "拍照失败")
                            }
                        }
                    }
                )
            }
        }
    )
}
