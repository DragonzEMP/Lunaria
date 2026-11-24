package com.lunaria.musicplayer.domain.model

import android.net.Uri

/**
 * Domain entity representing a music folder or container containing AudioTracks.
 */
data class MusicFolder(
    val name: String,
    val path: String,
    val tracks: List<AudioTrack>,
    val representativeArtUri: Uri? = null
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

