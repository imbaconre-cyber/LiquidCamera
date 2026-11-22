package com.example.liquidcamera

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun FocusRing(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(1.3f) }
    LaunchedEffect(visible) {
        if (visible) {
            alpha.snapTo(1f)
            scale.snapTo(1.3f)
            launch { scale.animateTo(1f, animationSpec = tween(durationMillis = 200)) }
            launch { alpha.animateTo(0f, animationSpec = tween(durationMillis = 1500, delayMillis = 300)) }
        }
    }
    if (alpha.value > 0f) {
        Box(
            modifier = modifier
                .size(80.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
                .border(
                    width = 2.dp,
                    color = Color(0xFFFFD60A),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                )
        )
    }
}