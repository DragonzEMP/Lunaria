package com.lunaria.musicplayer.domain.model

import android.net.Uri

/**
 * Domain entity representing a music album.
 */
data class MusicAlbum(
    val id: Long,
    val name: String,
    val artist: String,
    val tracks: List<AudioTrack>,
    val albumArtUri: Uri? = null,
    val year: Int = 0
) {
    val trackCount: Int
        get() = tracks.size

    val totalDurationFormatted: String
        get() {
            val totalMillis = tracks.sumOf { it.duration }
            if (totalMillis <= 0) return "0 mins"
            val totalMinutes = totalMillis / 60000
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes} mins"
        }
}

