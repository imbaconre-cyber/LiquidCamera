package com.example.liquidcamera

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.Locale

class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    val preview: Preview = Preview.Builder().build()
    val imageCapture: ImageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    var flashMode: Int = ImageCapture.FLASH_MODE_OFF
    var zoomRatio: Float = 1f

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null

    fun bind(previewView: PreviewView, onBound: () -> Unit = {}) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            cameraProvider = provider
            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                preview.surfaceProvider = previewView.surfaceProvider
                applyFlash()
                applyZoom()
                onBound()
            } catch (e: Exception) {
                Log.e("CameraController", "bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun rebind(previewView: PreviewView) {
        bind(previewView)
    }

    fun switchCamera(previewView: PreviewView) {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bind(previewView)
    }

    fun cycleFlash() {
        flashMode = when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }
        applyFlash()
    }

    private fun applyFlash() {
        imageCapture.flashMode = flashMode
    }

    fun setZoom(ratio: Float) {
        zoomRatio = ratio.coerceIn(
            camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f,
            camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 10f
        )
        applyZoom()
    }

    private fun applyZoom() {
        camera?.cameraControl?.setZoomRatio(zoomRatio)
    }

    fun tapToFocus(previewView: PreviewView, x: Float, y: Float) {
        val factory = previewView.meteringPointFactory
        val point = factory.createPoint(x, y)
        val action = androidx.camera.core.FocusMeteringAction.Builder(point).build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    fun takePhoto(onSaved: (android.net.Uri?) -> Unit) {
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_$name")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CameraApp")
        }
        val output = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()
        imageCapture.takePicture(
            output,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    onSaved(result.savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraController", "capture failed", exception)
                    onSaved(null)
                }
            }
        )
    }
}