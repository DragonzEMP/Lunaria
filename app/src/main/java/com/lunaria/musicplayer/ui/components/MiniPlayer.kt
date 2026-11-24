package com.lunaria.musicplayer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lunaria.musicplayer.R
import com.lunaria.musicplayer.player.PlaybackManager
import com.lunaria.musicplayer.ui.theme.MetallicMuted
import com.lunaria.musicplayer.ui.theme.MetallicText
import com.lunaria.musicplayer.ui.theme.PrimaryGlow
import com.lunaria.musicplayer.ui.theme.SecondaryAccent
import com.lunaria.musicplayer.ui.theme.SurfaceDark
import com.lunaria.musicplayer.ui.theme.SurfaceVariantDark
import kotlin.math.roundToInt

@Composable
fun MiniPlayer(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by PlaybackManager.currentTrack.collectAsState()
    val isPlaying by PlaybackManager.isPlaying.collectAsState()
    val currentPositionMs by PlaybackManager.currentPositionMs.collectAsState()
    val durationMs by PlaybackManager.durationMs.collectAsState()

    // Horizontal swipe gesture state to skip tracks
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val animatedDragX by animateFloatAsState(
        targetValue = if (isDragging) dragOffsetX else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "MiniPlayerSwipe"
    )

    val track = currentTrack ?: return // Don't show if no track has ever been loaded

    val maxDuration = if (durationMs > 0) durationMs else track.duration.coerceAtLeast(1L)
    val progressFraction = (currentPositionMs.toFloat() / maxDuration.toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .offset { IntOffset(animatedDragX.roundToInt(), 0) }
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(PrimaryGlow.copy(alpha = 0.5f), SecondaryAccent.copy(alpha = 0.4f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        if (dragOffsetX < -120f) {
                            // Swiped left -> skip next
                            PlaybackManager.skipToNext()
                        } else if (dragOffsetX > 120f) {
                            // Swiped right -> skip previous
                            PlaybackManager.skipToPrevious()
                        }
                        dragOffsetX = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragOffsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetX += dragAmount
                    }
                )
            },
        color = SurfaceDark.copy(alpha = 0.95f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art Thumbnail with glowing border
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceVariantDark)
                        .border(1.dp, SecondaryAccent.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(track.albumArtUri ?: track.uri)
                            .crossfade(true)
                            .build(),
                        placeholder = painterResource(id = R.drawable.ic_logo_lunaria),
                        error = painterResource(id = R.drawable.ic_logo_lunaria),
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = MetallicText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artist,
                        color = MetallicMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Play / Pause button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(PrimaryGlow, SecondaryAccent)
                            )
                        )
                        .clickable { PlaybackManager.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = SurfaceDark,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Thin glowing progress bar at the very bottom of the strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(PrimaryGlow.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .height(2.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(PrimaryGlow, SecondaryAccent)
                            )
                        )
                )
            }
        }
    }
}

