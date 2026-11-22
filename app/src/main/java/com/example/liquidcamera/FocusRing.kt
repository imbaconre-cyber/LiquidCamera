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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun FocusRing(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            alpha.snapTo(1f)
            launch {
                alpha.animateTo(0f, animationSpec = tween(durationMillis = 1500))
            }
        }
    }
    if (alpha.value > 0f) {
        Box(
            modifier = modifier
                .size(80.dp)
                .border(
                    width = 2.dp,
                    color = Color(0xFFFFD60A).copy(alpha = alpha.value),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                )
        )
    }
}