package com.lunaria.musicplayer.online.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lunaria.musicplayer.online.OnlineRepository
import com.lunaria.musicplayer.online.YouTubeSearchService
import com.lunaria.musicplayer.online.YouTubeStreamExtractor
import com.lunaria.musicplayer.online.model.OnlinePlaylist
import com.lunaria.musicplayer.online.model.OnlineTrack
import com.lunaria.musicplayer.player.PlaybackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class OnlineTab(val title: String) {
    SEARCH("Search"),
    SAVED("Saved"),
    PLAYLISTS("Playlists")
}

class OnlineViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OnlineRepository(application.applicationContext)
    private val searchService = YouTubeSearchService()

    // Tab state
    private val _selectedTab = MutableStateFlow(OnlineTab.SEARCH)
    val selectedTab: StateFlow<OnlineTab> = _selectedTab.asStateFlow()

    fun selectTab(tab: OnlineTab) {
        _selectedTab.value = tab
    }

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchResults = MutableStateFlow<List<OnlineTrack>>(emptyList())
    val searchResults: StateFlow<List<OnlineTrack>> = _searchResults.asStateFlow()

    private val _searchErrorMessage = MutableStateFlow<String?>(null)
    val searchErrorMessage: StateFlow<String?> = _searchErrorMessage.asStateFlow()

    // Stream Extraction state
    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    private val _extractingTrackTitle = MutableStateFlow<String?>(null)
    val extractingTrackTitle: StateFlow<String?> = _extractingTrackTitle.asStateFlow()

    private val _extractionErrorMessage = MutableStateFlow<String?>(null)
    val extractionErrorMessage: StateFlow<String?> = _extractionErrorMessage.asStateFlow()

    // History, Saved & Playlists from repository
    val searchHistory: StateFlow<List<String>> = repository.searchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedTracks: StateFlow<List<OnlineTrack>> = repository.savedTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<OnlinePlaylist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun performSearch(query: String = _searchQuery.value) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        _searchQuery.value = trimmed

        viewModelScope.launch {
            _isSearching.value = true
            _searchErrorMessage.value = null
            repository.addSearchQuery(trimmed)

            val result = searchService.searchMusic(trimmed)
            _isSearching.value = false
            result.onSuccess { tracks ->
                _searchResults.value = tracks
                if (tracks.isEmpty()) {
                    _searchErrorMessage.value = "No music videos found for \"$trimmed\""
                }
            }.onFailure { err ->
                _searchErrorMessage.value = err.message ?: "Failed to load results. Check network connection."
            }
        }
    }

    fun removeSearchHistory(query: String) {
        viewModelScope.launch {
            repository.removeSearchQuery(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    fun toggleSaveTrack(track: OnlineTrack) {
        viewModelScope.launch {
            repository.toggleSaveTrack(track)
        }
    }

    fun isTrackSaved(videoId: String): Boolean {
        return savedTracks.value.any { it.videoId == videoId }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addTrackToPlaylist(playlistId: String, track: OnlineTrack) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track)
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, videoId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, videoId)
        }
    }

    fun clearExtractionError() {
        _extractionErrorMessage.value = null
    }

    // Audio stream extraction and playback routing via PlaybackManager
    fun playTrack(track: OnlineTrack) {
        viewModelScope.launch {
            _isExtracting.value = true
            _extractingTrackTitle.value = track.title
            _extractionErrorMessage.value = null

            val result = YouTubeStreamExtractor.extractAudioStreamUrl(track.videoId)
            _isExtracting.value = false

            result.onSuccess { streamUrl ->
                PlaybackManager.playOnlineTrack(track, streamUrl)
            }.onFailure { error ->
                _extractionErrorMessage.value = error.message ?: "Failed to extract audio stream for ${track.title}"
            }
        }
    }
}
