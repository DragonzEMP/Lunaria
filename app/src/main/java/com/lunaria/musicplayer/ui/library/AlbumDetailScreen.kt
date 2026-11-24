package com.lunaria.musicplayer.ui.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.abs
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lunaria.musicplayer.R
import com.lunaria.musicplayer.domain.model.AudioTrack
import com.lunaria.musicplayer.domain.model.MusicAlbum
import com.lunaria.musicplayer.player.PlaybackManager
import com.lunaria.musicplayer.ui.theme.BackgroundDark
import com.lunaria.musicplayer.ui.theme.CopyrightColor
import com.lunaria.musicplayer.ui.theme.MetallicMuted
import com.lunaria.musicplayer.ui.theme.MetallicText
import com.lunaria.musicplayer.ui.theme.PrimaryGlow
import com.lunaria.musicplayer.ui.theme.SecondaryAccent
import com.lunaria.musicplayer.ui.theme.SurfaceDark
import com.lunaria.musicplayer.ui.theme.SurfaceVariantDark

@Composable
fun AlbumDetailScreen(
    album: MusicAlbum,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSwipeBack: ((direction: Int) -> Unit)? = null
) {
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDragOffset = 0f },
                    onDragEnd = {
                        if (abs(horizontalDragOffset) > 100f) {
                            val dir = if (horizontalDragOffset > 0) 1 else -1
                            if (onSwipeBack != null) onSwipeBack(dir) else onBackClick()
                        }
                        horizontalDragOffset = 0f
                    },
                    onDragCancel = { horizontalDragOffset = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        horizontalDragOffset += dragAmount
                        if (abs(horizontalDragOffset) > 120f) {
                            change.consume()
                            val dir = if (horizontalDragOffset > 0) 1 else -1
                            if (onSwipeBack != null) onSwipeBack(dir) else onBackClick()
                            horizontalDragOffset = 0f
                        }
                    }
                )
            }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Parallax / Blurred Artwork Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    // Blurred background artwork atmosphere
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(album.albumArtUri ?: album.tracks.firstOrNull()?.uri)
                            .crossfade(true)
                            .build(),
                        placeholder = painterResource(id = R.drawable.ic_logo_lunaria),
                        error = painterResource(id = R.drawable.ic_logo_lunaria),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(50.dp)
                            .background(Color.Black.copy(alpha = 0.5f))
                    )

                    // Gradient overlay to fade smoothly into BackgroundDark
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        BackgroundDark.copy(alpha = 0.6f),
                                        BackgroundDark
                                    )
                                )
                            )
                    )

                    // Centered album card content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(top = 40.dp, start = 20.dp, end = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(SurfaceDark)
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.linearGradient(
                                        listOf(PrimaryGlow, SecondaryAccent)
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(album.albumArtUri ?: album.tracks.firstOrNull()?.uri)
                                    .crossfade(true)
                                    .build(),
                                placeholder = painterResource(id = R.drawable.ic_logo_lunaria),
                                error = painterResource(id = R.drawable.ic_logo_lunaria),
                                contentDescription = album.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = album.name,
                            color = MetallicText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${album.artist} • ${album.trackCount} Tracks • ${album.totalDurationFormatted}",
                            color = SecondaryAccent.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Play All & Shuffle Action Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (album.tracks.isNotEmpty()) {
                                PlaybackManager.playTrack(album.tracks.first(), album.tracks)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGlow),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PLAY ALL", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            if (album.tracks.isNotEmpty()) {
                                PlaybackManager.toggleShuffle()
                                PlaybackManager.playTrack(album.tracks.random(), album.tracks)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, SecondaryAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, tint = SecondaryAccent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SHUFFLE", fontWeight = FontWeight.SemiBold, color = SecondaryAccent)
                    }
                }
            }

            // Track List Items showing track number, duration, and file format pill
            itemsIndexed(album.tracks, key = { _, track -> track.id }) { index, track ->
                AlbumTrackRow(
                    index = index + 1,
                    track = track,
                    onClick = {
                        PlaybackManager.playTrack(track, album.tracks)
                    }
                )
            }
        }

        // Back button floating at top
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceDark.copy(alpha = 0.8f))
                .border(1.dp, PrimaryGlow.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = SecondaryAccent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AlbumTrackRow(
    index: Int,
    track: AudioTrack,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = SurfaceDark.copy(alpha = 0.65f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track Number (e.g. "01", "02")
            Text(
                text = String.format("%02d", index),
                color = PrimaryGlow,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(28.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Album Artwork Thumbnail for each track
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariantDark)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(PrimaryGlow.copy(alpha = 0.5f), SecondaryAccent.copy(alpha = 0.4f))
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ),
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

            // Title & Artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = MetallicText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    color = MetallicMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // File format pill (e.g. "MP3", "FLAC")
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceVariantDark)
                    .border(0.8.dp, SecondaryAccent.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = track.format,
                    color = SecondaryAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Duration
            Text(
                text = track.formattedDuration,
                color = CopyrightColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

