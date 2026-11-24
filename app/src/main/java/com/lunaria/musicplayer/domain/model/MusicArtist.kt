package com.lunaria.musicplayer.domain.model

import android.net.Uri

/**
 * Domain entity representing an artist.
 */
data class MusicArtist(
    val name: String,
    val tracks: List<AudioTrack>,
    val representativeArtUri: Uri? = null
) {
    val trackCount: Int
        get() = tracks.size

    val albumCount: Int
        get() = tracks.map { it.album }.distinct().size
}

