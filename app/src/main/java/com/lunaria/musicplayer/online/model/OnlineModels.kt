package com.lunaria.musicplayer.online.model

data class OnlineTrack(
    val videoId: String,
    val title: String,
    val channel: String,
    val thumbnailUrl: String
)

data class OnlinePlaylist(
    val id: String,
    val name: String,
    val tracks: List<OnlineTrack> = emptyList()
)
