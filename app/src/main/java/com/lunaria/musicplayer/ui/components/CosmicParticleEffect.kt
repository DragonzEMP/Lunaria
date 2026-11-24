package com.lunaria.musicplayer.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.lunaria.musicplayer.ui.theme.MetallicText
import com.lunaria.musicplayer.ui.theme.PrimaryGlow
import com.lunaria.musicplayer.ui.theme.SecondaryAccent
import kotlin.random.Random

private data class CosmicParticle(
    val initialX: Float,
    val initialY: Float,
    val radius: Float,
    val speed: Float,
    val alphaOffset: Float,
    val color: Color
)

@Composable
fun CosmicParticleEffect(
    modifier: Modifier = Modifier,
    particleCount: Int = 45
) {
    val particles = remember(particleCount) {
        val colors = listOf(
            SecondaryAccent,
            PrimaryGlow,
            MetallicText,
            Color.White
        )
        val rng = Random(42)
        List(particleCount) {
            CosmicParticle(
                initialX = rng.nextFloat(),
                initialY = rng.nextFloat(),
                radius = rng.nextFloat() * 2.2f + 0.8f,
                speed = rng.nextFloat() * 0.4f + 0.15f,
                alphaOffset = rng.nextFloat() * 6.28f,
                color = colors[rng.nextInt(colors.size)]
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "CosmicDrift")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CosmicParticleProgress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { p ->
            val curY = ((p.initialY - progress * p.speed) % 1f + 1f) % 1f * height
            val curX = (p.initialX * width + kotlin.math.sin(progress * 6.28f + p.alphaOffset).toFloat() * 12f)
            val twinkle = (kotlin.math.sin((progress * 12.56f + p.alphaOffset).toDouble()).toFloat() + 1f) / 2f
            val alpha = (0.2f + twinkle * 0.65f).coerceIn(0.1f, 0.85f)

            // Draw soft outer glow
            drawCircle(
                color = p.color.copy(alpha = alpha * 0.35f),
                radius = p.radius * 2.5f,
                center = Offset(curX, curY)
            )
            // Draw core particle
            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.radius,
                center = Offset(curX, curY)
            )
        }
    }
}

