package com.lunaria.musicplayer.ui.player

import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lunaria.musicplayer.R
import com.lunaria.musicplayer.domain.model.AudioTrack
import com.lunaria.musicplayer.online.OnlineRepository
import com.lunaria.musicplayer.player.PlaybackManager
import kotlinx.coroutines.launch
import com.lunaria.musicplayer.ui.components.CosmicParticleEffect
import com.lunaria.musicplayer.ui.components.NeonVisualizer
import com.lunaria.musicplayer.ui.theme.BackgroundDark
import com.lunaria.musicplayer.ui.theme.CopyrightColor
import com.lunaria.musicplayer.ui.theme.MetallicMuted
import com.lunaria.musicplayer.ui.theme.MetallicText
import com.lunaria.musicplayer.ui.theme.PrimaryGlow
import com.lunaria.musicplayer.ui.theme.SecondaryAccent
import com.lunaria.musicplayer.ui.theme.SurfaceDark
import com.lunaria.musicplayer.ui.theme.SurfaceVariantDark
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onDismiss: () -> Unit,
    onNavigateToEqualizer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val currentTrack by PlaybackManager.currentTrack.collectAsState()
    val currentOnlineTrack by PlaybackManager.currentOnlineTrack.collectAsState()
    val onlineRepo = remember { OnlineRepository(context) }
    val savedOnlineTracks by onlineRepo.savedTracks.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    val isPlaying by PlaybackManager.isPlaying.collectAsState()
    val currentPositionMs by PlaybackManager.currentPositionMs.collectAsState()
    val durationMs by PlaybackManager.durationMs.collectAsState()
    val isShuffle by PlaybackManager.isShuffle.collectAsState()
    val repeatMode by PlaybackManager.repeatMode.collectAsState()

    // Vertical swipe-down dismissal gestures
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isDragging) dragOffsetY else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "SwipeDismissY"
    )

    // Scrubbing state
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }

    val effectiveTrack = currentTrack ?: AudioTrack(
        id = 0,
        title = "Celestia Aurora",
        artist = "Lunaria Synth Orchestra",
        album = "Starlight Odyssey",
        duration = 240000,
        uri = android.net.Uri.EMPTY,
        format = "FLAC",
        bitrate = "320 kbps",
        sampleRate = "44.1 kHz"
    )

    val maxDuration = if (durationMs > 0) durationMs else effectiveTrack.duration.coerceAtLeast(1L)
    val currentPos = if (isScrubbing) (scrubPosition * maxDuration).toLong() else currentPositionMs
    val progressFraction = (currentPos.toFloat() / maxDuration.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
            .alpha(1f - (animatedOffsetY / 900f).coerceIn(0f, 0.45f))
            .background(BackgroundDark)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragOffsetY = 0f
                    },
                    onDragEnd = {
                        isDragging = false
                        if (dragOffsetY > 150f) {
                            onDismiss()
                        }
                        dragOffsetY = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragOffsetY = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        // Only track downward gestures to dismiss
                        if (dragAmount > 0f || dragOffsetY > 0f) {
                            dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                            if (dragOffsetY > 150f) {
                                change.consume()
                                isDragging = false
                                onDismiss()
                                dragOffsetY = 0f
                            }
                        }
                    }
                )
            }
    ) {
        // Celestial particle field background
        CosmicParticleEffect(
            modifier = Modifier.fillMaxSize(),
            particleCount = 35
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Header Row: Dismiss arrow, screen title, menu icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark.copy(alpha = 0.8f))
                        .border(1.dp, PrimaryGlow.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = SecondaryAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = "NOW PLAYING",
                    color = MetallicText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark.copy(alpha = 0.8f))
                            .border(1.dp, PrimaryGlow.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = SecondaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(SurfaceDark)
                            .border(1.dp, PrimaryGlow.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Song Info", color = MetallicText) },
                            onClick = {
                                showMenu = false
                                Toast.makeText(
                                    context,
                                    "${effectiveTrack.title}\n${effectiveTrack.artist} • ${effectiveTrack.album} (${effectiveTrack.format})",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Playlist", color = MetallicText) },
                            onClick = {
                                showMenu = false
                                Toast.makeText(context, "Add to Playlist coming soon", Toast.LENGTH_SHORT).show()
                            }
                        )
                        if (currentOnlineTrack != null) {
                            val isSaved = savedOnlineTracks.any { it.videoId == currentOnlineTrack?.videoId }
                            DropdownMenuItem(
                                text = { Text(if (isSaved) "Remove from Saved" else "Save to Online Library", color = MetallicText) },
                                onClick = {
                                    showMenu = false
                                    coroutineScope.launch {
                                        currentOnlineTrack?.let { track ->
                                            val isNowSaved = onlineRepo.toggleSaveTrack(track)
                                            Toast.makeText(
                                                context,
                                                if (isNowSaved) "Saved to Online Library" else "Removed from Saved",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Massive Centered Album Artwork with PrimaryGlow drop shadow
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .shadow(
                        elevation = 28.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = PrimaryGlow,
                        ambientColor = PrimaryGlow
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(SurfaceDark)
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            listOf(PrimaryGlow, SecondaryAccent)
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(effectiveTrack.albumArtUri ?: effectiveTrack.uri)
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(id = R.drawable.ic_logo_lunaria),
                    error = painterResource(id = R.drawable.ic_logo_lunaria),
                    contentDescription = effectiveTrack.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Track Title (Large, Bold) and Artist / Album Metadata
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = effectiveTrack.title,
                    color = MetallicText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${effectiveTrack.artist} • ${effectiveTrack.album}",
                    color = MetallicMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            // 4. Utility Icons Row (Shuffle, Repeat, Sleep Timer, Lyrics)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { PlaybackManager.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) SecondaryAccent else CopyrightColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = { PlaybackManager.cycleRepeatMode() }) {
                    Icon(
                        imageVector = if (repeatMode == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = when (repeatMode) {
                            2 -> "Repeat One (Loop single track)"
                            1 -> "Repeat All (Loop album)"
                            else -> "Repeat Off (Stop after album)"
                        },
                        tint = when (repeatMode) {
                            2 -> PrimaryGlow
                            1 -> SecondaryAccent
                            else -> CopyrightColor.copy(alpha = 0.45f)
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = {
                    Toast.makeText(context, "Sleep Timer coming soon", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Sleep Timer",
                        tint = CopyrightColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (currentOnlineTrack != null) {
                    val isSaved = savedOnlineTracks.any { it.videoId == currentOnlineTrack?.videoId }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            currentOnlineTrack?.let { track ->
                                val isNowSaved = onlineRepo.toggleSaveTrack(track)
                                Toast.makeText(
                                    context,
                                    if (isNowSaved) "Saved to Online Library" else "Removed from Saved",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isSaved) "Remove from Saved" else "Save to Online Library",
                            tint = if (isSaved) SecondaryAccent else CopyrightColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                IconButton(onClick = onNavigateToEqualizer) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Equalizer",
                        tint = SecondaryAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Live Audio Reactive Neon Visualizer
            NeonVisualizer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                height = 54.dp
            )

            // 5. Thick Glowing Seekbar with Timestamps & Audio Format details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Slider(
                    value = progressFraction,
                    onValueChange = { fraction ->
                        isScrubbing = true
                        scrubPosition = fraction
                    },
                    onValueChangeFinished = {
                        val targetMs = (scrubPosition * maxDuration).toLong()
                        PlaybackManager.seekTo(targetMs)
                        isScrubbing = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = SecondaryAccent,
                        activeTrackColor = SecondaryAccent,
                        inactiveTrackColor = PrimaryGlow.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current time
                    Text(
                        text = formatTime(currentPos),
                        color = SecondaryAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Audio format details centered in the middle
                    Text(
                        text = "${effectiveTrack.sampleRate} • ${effectiveTrack.bitrate} • ${effectiveTrack.format}",
                        color = CopyrightColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )

                    // Total time
                    Text(
                        text = formatTime(maxDuration),
                        color = MetallicMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 6. Main Playback Controls: Previous, Rewind, Large glowing Play/Pause, Fast-forward, Next
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous
                IconButton(
                    onClick = { PlaybackManager.skipToPrevious() },
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MetallicText,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Rewind 10s
                IconButton(
                    onClick = { PlaybackManager.seekBackward(10000L) },
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10 seconds",
                        tint = MetallicMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Large Glowing Play/Pause button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(
                            elevation = 20.dp,
                            shape = CircleShape,
                            spotColor = PrimaryGlow,
                            ambientColor = SecondaryAccent
                        )
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
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Fast-forward 10s
                IconButton(
                    onClick = { PlaybackManager.seekForward(10000L) },
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10 seconds",
                        tint = MetallicMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Next
                IconButton(
                    onClick = { PlaybackManager.skipToNext() },
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MetallicText,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

