package com.example.liquidcamera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class CameraMode { VIDEO, PHOTO, PORTRAIT }

private val Modes = listOf(CameraMode.VIDEO, CameraMode.PHOTO, CameraMode.PORTRAIT)

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { CameraController(context, lifecycleOwner) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var mode by remember { mutableStateOf(CameraMode.PHOTO) }
    var lastPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var focusOffset by remember { mutableStateOf<Offset?>(null) }
    var focusVisible by remember { mutableStateOf(false) }
    var showZoomSlider by remember { mutableStateOf(false) }

    val safeTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val safeBottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()

    val backdrop = rememberLayerBackdrop {
        drawRect(Color.Black)
        drawContent()
    }

    val thumbnailBitmap = remember(lastPhotoUri) {
        lastPhotoUri?.let { loadThumbnail(context, it) }
    }

    LaunchedEffect(focusVisible) {
        if (focusVisible) {
            delay(1500)
            focusVisible = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Preview layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                update = { pv ->
                    if (previewView == null) {
                        previewView = pv
                        controller.bind(pv)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            val newZoom = (zoomRatio * zoom).coerceIn(0.5f, 10f)
                            zoomRatio = newZoom
                            controller.setZoom(newZoom)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            focusOffset = offset
                            focusVisible = true
                            previewView?.let { controller.tapToFocus(it, offset.x, offset.y) }
                        }
                    }
            )
        }

        // Focus ring
        focusOffset?.let { off ->
            if (focusVisible) {
                FocusRing(
                    visible = true,
                    modifier = Modifier.offset {
                        IntOffset(
                            (off.x - 40.dp.toPx()).roundToInt(),
                            (off.y - 40.dp.toPx()).roundToInt()
                        )
                    }
                )
            }
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = safeTop + 16.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidButton(
                onClick = {
                    controller.cycleFlash()
                    flashMode = controller.flashMode
                },
                backdrop = backdrop
            ) {
                Text(
                    text = when (flashMode) {
                        ImageCapture.FLASH_MODE_OFF -> "Off"
                        ImageCapture.FLASH_MODE_ON -> "On"
                        else -> "Auto"
                    },
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
            LiquidButton(
                onClick = {
                    previewView?.let { controller.switchCamera(it) }
                },
                backdrop = backdrop
            ) {
                Text(text = "Flip", color = Color.White, fontSize = 12.sp)
            }
        }

        // Zoom pill + slider + mode switcher column
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = safeBottom + 120.dp + 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showZoomSlider) {
                LiquidPill(backdrop = backdrop, modifier = Modifier.width(240.dp)) {
                    Slider(
                        value = zoomRatio,
                        onValueChange = {
                            zoomRatio = it
                            controller.setZoom(it)
                        },
                        valueRange = 0.5f..10f,
                        modifier = Modifier.width(220.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            LiquidPill(
                backdrop = backdrop,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = { showZoomSlider = !showZoomSlider })
                }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(0.5f, 1f, 2f).forEach { level ->
                        Text(
                            text = formatZoom(level),
                            color = if (zoomRatio == level) Color.White else Color(0xFF8E8E93),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                zoomRatio = level
                                controller.setZoom(level)
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Modes.forEach { m ->
                    Text(
                        text = m.name,
                        color = if (mode == m) Color.White else Color(0xFF8E8E93),
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { mode = m }
                    )
                }
            }
        }

        // Bottom bar
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            LiquidBottomBar(backdrop = backdrop) {}
        }

        // Thumbnail
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = safeBottom + (120.dp - 56.dp) / 2)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                thumbnailBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Last photo",
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }
            }
        }

        // Shutter
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val shutterScale by animateFloatAsState(if (isPressed) 0.85f else 1f, label = "shutter")
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = safeBottom + 32.dp)
                .scale(shutterScale)
                .size(76.dp)
                .border(4.dp, Color.White, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    controller.takePhoto { uri -> lastPhotoUri = uri }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color.White.copy(alpha = 0.0f), CircleShape)
            )
        }
    }
}

private fun formatZoom(level: Float): String {
    return if (level == 1f) "1×" else "${level}×"
}

private fun loadThumbnail(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) {
        null
    }
}