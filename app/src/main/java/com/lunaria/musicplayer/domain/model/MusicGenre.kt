package com.lunaria.musicplayer.domain.model

/**
 * Domain entity representing a music genre.
 */
data class MusicGenre(
    val name: String,
    val tracks: List<AudioTrack>
) {
    val trackCount: Int
        get() = tracks.size
}

