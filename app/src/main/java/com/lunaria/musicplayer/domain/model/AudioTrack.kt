package com.lunaria.musicplayer.domain.model

import android.net.Uri

/**
 * Domain entity representing an audio track discovered on the device.
 */
data class AudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: Uri,
    val albumArtUri: Uri? = null,
    val folderName: String = "Music",
    val folderPath: String = "",
    val trackNumber: Int = 1,
    val format: String = "MP3",
    val bitrate: String = "320 kbps",
    val sampleRate: String = "44.1 kHz",
    val year: Int = 0
) {
    /**
     * Formats duration in milliseconds to a human-readable mm:ss or hh:mm:ss string.
     */
    val formattedDuration: String
        get() {
            if (duration <= 0) return "0:00"
            val totalSeconds = duration / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60

            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    /**
     * Formats track number as two-digit string e.g. "01", "07".
     */
    val formattedTrackNumber: String
        get() = String.format("%02d", if (trackNumber > 0) trackNumber else 1)
}
