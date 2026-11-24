package com.lunaria.musicplayer.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.lunaria.musicplayer.domain.model.AudioTrack
import com.lunaria.musicplayer.online.model.OnlineTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Singleton Audio Playback Controller managing ExoPlayer lifecycle and state across the UI.
 */
object PlaybackManager {

    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _currentTrack = MutableStateFlow<AudioTrack?>(null)
    val currentTrack: StateFlow<AudioTrack?> = _currentTrack.asStateFlow()

    private val _currentOnlineTrack = MutableStateFlow<OnlineTrack?>(null)
    val currentOnlineTrack: StateFlow<OnlineTrack?> = _currentOnlineTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playlist = MutableStateFlow<List<AudioTrack>>(emptyList())
    val playlist: StateFlow<List<AudioTrack>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    // 0: Off, 1: Repeat All, 2: Repeat One
    private val _repeatMode = MutableStateFlow(1)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    fun getAudioSessionId(): Int = exoPlayer?.audioSessionId ?: 0

    fun initialize(context: Context) {
        if (exoPlayer != null) return

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        exoPlayer = ExoPlayer.Builder(context.applicationContext)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                setAudioAttributes(audioAttributes, true)
                volume = 1.0f
                repeatMode = when (_repeatMode.value) {
                    2 -> Player.REPEAT_MODE_ONE
                    1 -> Player.REPEAT_MODE_ALL
                    else -> Player.REPEAT_MODE_OFF
                }
                shuffleModeEnabled = _isShuffle.value
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                        VisualizerManager.setPlaybackActive(playing)
                        if (playing) {
                            startProgressUpdates()
                        } else {
                            stopProgressUpdates()
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _durationMs.value = duration.coerceAtLeast(0L)
                            val sessionId = audioSessionId
                            if (sessionId > 0) {
                                AudioFxManager.attachToAudioSession(sessionId)
                            }
                        } else if (playbackState == Player.STATE_ENDED) {
                            if (_repeatMode.value != 0) {
                                skipToNext()
                            }
                        }
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        val currentIdx = currentMediaItemIndex
                        _currentIndex.value = currentIdx
                        val list = _playlist.value
                        if (currentIdx in list.indices) {
                            _currentTrack.value = list[currentIdx]
                        }
                    }
                })

                val initialSessionId = audioSessionId
                if (initialSessionId > 0) {
                    AudioFxManager.attachToAudioSession(initialSessionId)
                }
            }
    }

    fun playTrack(track: AudioTrack, newPlaylist: List<AudioTrack> = listOf(track)) {
        val player = exoPlayer ?: return
        _currentOnlineTrack.value = null
        _playlist.value = newPlaylist
        val index = newPlaylist.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        _currentIndex.value = index
        _currentTrack.value = track

        val mediaItems = newPlaylist.map { t ->
            val metadata = androidx.media3.common.MediaMetadata.Builder()
                .setTitle(t.title)
                .setArtist(t.artist)
                .setAlbumTitle(t.album)
                .setArtworkUri(t.albumArtUri)
                .build()

            MediaItem.Builder()
                .setUri(t.uri)
                .setMediaId(t.id.toString())
                .setMediaMetadata(metadata)
                .build()
        }

        player.setMediaItems(mediaItems, index, 0L)
        player.prepare()
        player.play()
    }

    fun playOnlineTrack(track: OnlineTrack, streamUrl: String) {
        val player = exoPlayer ?: return

        _currentOnlineTrack.value = track

        // Automatically turn EQ OFF by default in online mode unless user manually enabled it during this session
        if (!AudioFxManager.userManuallyEnabledEqInOnline) {
            AudioFxManager.setEnabled(false)
            AudioFxManager.setPreampGain(AudioFxManager.BASELINE_PREAMP_GAIN_MB)
            setMasterVolume(1.0f)
        }

        val artworkUri = if (track.thumbnailUrl.isNotBlank()) Uri.parse(track.thumbnailUrl) else null
        val streamUri = Uri.parse(streamUrl)

        val audioTrack = AudioTrack(
            id = track.videoId.hashCode().toLong(),
            title = track.title,
            artist = track.channel,
            album = "YouTube Music",
            duration = 0L,
            uri = streamUri,
            albumArtUri = artworkUri,
            folderName = "Online Stream",
            format = "M4A/OPUS",
            bitrate = "Adaptive"
        )

        _playlist.value = listOf(audioTrack)
        _currentIndex.value = 0
        _currentTrack.value = audioTrack

        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.channel)
            .setAlbumTitle("YouTube Music")
            .setArtworkUri(artworkUri)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(streamUri)
            .setMediaId(track.videoId)
            .setMediaMetadata(metadata)
            .build()

        player.setMediaItem(mediaItem, 0L)
        player.prepare()
        player.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (_currentTrack.value != null) {
                player.play()
            }
        }
    }

    fun skipToNext() {
        val player = exoPlayer ?: return
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else {
            val list = _playlist.value
            if (list.isNotEmpty() && _repeatMode.value == 1) {
                val nextIdx = if (_isShuffle.value) {
                    list.indices.random()
                } else {
                    0
                }
                player.seekTo(nextIdx, 0L)
            }
        }
    }

    fun skipToPrevious() {
        val player = exoPlayer ?: return
        if (player.currentPosition > 3000L) {
            player.seekTo(0)
            return
        }
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            val list = _playlist.value
            if (list.isNotEmpty()) {
                val prevIdx = list.size - 1
                player.seekTo(prevIdx, 0L)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        player.seekTo(positionMs.coerceIn(0L, _durationMs.value))
        _currentPositionMs.value = positionMs
    }

    fun seekForward(deltaMs: Long = 10000L) {
        val player = exoPlayer ?: return
        val newPos = (player.currentPosition + deltaMs).coerceAtMost(player.duration)
        player.seekTo(newPos)
    }

    fun seekBackward(deltaMs: Long = 10000L) {
        val player = exoPlayer ?: return
        val newPos = (player.currentPosition - deltaMs).coerceAtLeast(0L)
        player.seekTo(newPos)
    }

    fun toggleShuffle() {
        val newShuffle = !_isShuffle.value
        _isShuffle.value = newShuffle
        exoPlayer?.shuffleModeEnabled = newShuffle
    }

    fun cycleRepeatMode() {
        val nextMode = (_repeatMode.value + 1) % 3
        _repeatMode.value = nextMode
        exoPlayer?.repeatMode = when (nextMode) {
            2 -> Player.REPEAT_MODE_ONE
            1 -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setMasterVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0.0f, 1.0f)
    }

    fun getMasterVolume(): Float = exoPlayer?.volume ?: 1.0f

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    _currentPositionMs.value = player.currentPosition.coerceAtLeast(0L)
                    if (player.duration > 0) {
                        _durationMs.value = player.duration
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }
}

