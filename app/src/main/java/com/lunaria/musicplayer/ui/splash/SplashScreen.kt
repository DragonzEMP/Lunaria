package com.lunaria.musicplayer.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lunaria.musicplayer.R
import com.lunaria.musicplayer.ui.components.CosmicParticleEffect
import com.lunaria.musicplayer.ui.theme.BackgroundDark
import com.lunaria.musicplayer.ui.theme.CopyrightColor
import com.lunaria.musicplayer.ui.theme.ElectricViolet
import com.lunaria.musicplayer.ui.theme.MetallicText
import com.lunaria.musicplayer.ui.theme.PrimaryGlow
import com.lunaria.musicplayer.ui.theme.SecondaryAccent

import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onTimeout: () -> Unit = {}
) {
    // Strict 2-second (2000ms) splash window
    LaunchedEffect(Unit) {
        delay(2000L)
        onTimeout()
    }
    val infiniteTransition = rememberInfiniteTransition(label = "SplashAnimations")

    // Subtle celestial breathing animation synced to the 2-second window
    val emblemScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EmblemBreathing"
    )

    // Pulsing halo glow behind the emblem
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HaloGlowPulse"
    )

    // Rotating progress angle for custom scanning indicator (completes 2 full rotations in 2000ms)
    val progressRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ScanningIndicatorRotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // 1. Cosmic Background with Nebula Radial Gradients
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width / 2f, size.height * 0.42f)
            val glowRadius = size.width * 0.85f

            // Core Nebula Glow (Purple -> Electric Blue -> Void)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PrimaryGlow.copy(alpha = haloPulse * 0.45f),
                        ElectricViolet.copy(alpha = haloPulse * 0.25f),
                        SecondaryAccent.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = glowRadius
                ),
                center = centerOffset,
                radius = glowRadius
            )
        }

        // 2. Faint Particle Effect Overlay for cosmic depth
        CosmicParticleEffect(
            modifier = Modifier.fillMaxSize(),
            particleCount = 50
        )

        // 3. Centered Brand Content: Emblem + Typography + Scanning Indicator
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .statusBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing Moon Emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp)
            ) {
                // Background radial flare aligned with emblem
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SecondaryAccent.copy(alpha = haloPulse * 0.35f),
                                PrimaryGlow.copy(alpha = haloPulse * 0.2f),
                                Color.Transparent
                            ),
                            radius = size.minDimension * 0.6f
                        )
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.ic_logo_lunaria),
                    contentDescription = stringResource(id = R.string.logo_content_desc),
                    modifier = Modifier
                        .size(190.dp)
                        .scale(emblemScale)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Glowing Custom Typography "LUNARIA"
            // High-resolution extracted brand lettering
            Image(
                painter = painterResource(id = R.drawable.ic_lunaria_text),
                contentDescription = stringResource(id = R.string.app_name),
                modifier = Modifier
                    .height(34.dp)
                    .scale(emblemScale * 0.98f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Animated Custom Circular Progress Indicator (SecondaryAccent Gradient)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(54.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 3.5.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                    val arcSize = Size(diameter, diameter)

                    // Track background ring
                    drawArc(
                        color = SecondaryAccent.copy(alpha = 0.12f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )

                    // Animated Glowing Gradient Arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            0.0f to Color.Transparent,
                            0.5f to PrimaryGlow.copy(alpha = 0.5f),
                            0.85f to SecondaryAccent,
                            1.0f to Color.White
                        ),
                        startAngle = progressRotation,
                        sweepAngle = 260f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Scanning indicator status label
            Text(
                text = stringResource(id = R.string.scanning_media),
                color = SecondaryAccent.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(0.9f)
            )
        }

        // 4. Snugly Pinned Bottom Copyright Notice & Tagline
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.app_tagline),
                color = CopyrightColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(id = R.string.copyright_notice),
                color = CopyrightColor.copy(alpha = 0.75f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF080A15)
@Composable
fun SplashScreenPreview() {
    SplashScreen()
}

