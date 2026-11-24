package com.lunaria.musicplayer.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lunaria.musicplayer.player.VisualizerManager
import com.lunaria.musicplayer.ui.theme.LocalThemeColors
import com.lunaria.musicplayer.ui.theme.PrimaryGlow
import com.lunaria.musicplayer.ui.theme.SecondaryAccent

@Composable
fun NeonVisualizer(
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    barSpacing: Dp = 3.dp,
    primaryColor: Color = LocalThemeColors.current.primaryGlow,
    secondaryColor: Color = LocalThemeColors.current.secondaryAccent
) {
    val magnitudes by VisualizerManager.magnitudes.collectAsState()

    // Animate each bar height with animateFloatAsState so bars jump instantaneously on beats and fall smoothly
    val barCount = VisualizerManager.NUM_BARS
    val animatedMagnitudes = List(barCount) { index ->
        val rawMag = magnitudes.getOrElse(index) { 0.04f }.coerceIn(0.04f, 1f)
        animateFloatAsState(
            targetValue = rawMag,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessHigh
            ),
            label = "vis_bar_$index"
        ).value
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val count = animatedMagnitudes.size
        if (count == 0) return@Canvas

        val spacingPx = barSpacing.toPx()
        val totalSpacing = spacingPx * (count - 1)
        val barWidthPx = ((size.width - totalSpacing) / count).coerceAtLeast(2f)
        val canvasHeight = size.height

        val gradientBrush = Brush.verticalGradient(
            colors = listOf(
                secondaryColor,
                primaryColor.copy(alpha = 0.85f),
                primaryColor.copy(alpha = 0.3f)
            ),
            startY = 0f,
            endY = canvasHeight
        )

        for (i in 0 until count) {
            val magnitude = animatedMagnitudes[i].coerceIn(0.04f, 1f)
            val barHeight = (magnitude * canvasHeight).coerceAtLeast(4.dp.toPx())
            val x = i * (barWidthPx + spacingPx)
            val y = canvasHeight - barHeight

            // 1. Soft Outer Neon Glow Layer
            drawRoundRect(
                color = secondaryColor.copy(alpha = 0.25f * magnitude),
                topLeft = Offset(x - 2f, y - 2f),
                size = Size(barWidthPx + 4f, barHeight + 2f),
                cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)
            )

            // 2. Core Gradient Visualizer Bar
            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(x, y),
                size = Size(barWidthPx, barHeight),
                cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)
            )

            // 3. Bright Peak Tip Flare
            if (magnitude > 0.25f) {
                drawCircle(
                    color = Color.White.copy(alpha = (magnitude * 0.9f).coerceIn(0.2f, 1f)),
                    radius = (barWidthPx / 2.2f).coerceAtMost(3.dp.toPx()),
                    center = Offset(x + barWidthPx / 2f, y + 2.dp.toPx())
                )
            }
        }
    }
}
