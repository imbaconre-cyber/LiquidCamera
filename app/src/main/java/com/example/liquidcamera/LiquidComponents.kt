package com.example.liquidcamera

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule

// Liquid Glass constants
val LiquidBlurRadius = 4.dp
val LiquidLensHeight = 16.dp
val LiquidLensAmount = 32.dp
val LiquidVibrancyEnabled = true

private val TranslucentDark = Color.Black.copy(alpha = 0.45f)

private fun isBlurSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
private fun Modifier.glassSurface(
    backdrop: Backdrop,
    shape: androidx.compose.ui.graphics.Shape
): Modifier {
    return if (isBlurSupported()) {
        this.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(LiquidBlurRadius.toPx())
                // lens requires CornerBasedShape; only apply when possible
            }
        )
    } else {
        this.background(TranslucentDark, shape)
    }
}

// LiquidButton: circular glass button
@Composable
fun LiquidButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (isBlurSupported()) Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { CircleShape },
                    effects = {
                        vibrancy()
                        blur(LiquidBlurRadius.toPx())
                        lens(LiquidLensHeight.toPx(), LiquidLensAmount.toPx())
                    }
                ) else Modifier.background(TranslucentDark, CircleShape)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// LiquidPill: capsule glass, 40dp tall
@Composable
fun LiquidPill(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .then(
                if (isBlurSupported()) Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(LiquidBlurRadius.toPx())
                        lens(LiquidLensHeight.toPx(), LiquidLensAmount.toPx())
                    }
                ) else Modifier.background(TranslucentDark, Capsule())
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// LiquidBottomBar: full-width glass bar ~120dp tall, top corners rounded 32dp
@Composable
fun LiquidBottomBar(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val barShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .then(
                if (isBlurSupported()) Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { barShape },
                    effects = {
                        vibrancy()
                        blur(LiquidBlurRadius.toPx())
                        lens(LiquidLensHeight.toPx(), LiquidLensAmount.toPx())
                    }
                ) else Modifier.background(TranslucentDark, barShape)
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

