package com.lunaria.musicplayer.ui.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunaria.musicplayer.data.repository.AudioRepository
import com.lunaria.musicplayer.domain.model.AudioTrack
import com.lunaria.musicplayer.domain.model.MusicAlbum
import com.lunaria.musicplayer.domain.model.MusicArtist
import com.lunaria.musicplayer.domain.model.MusicFolder
import com.lunaria.musicplayer.domain.model.MusicGenre
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Top-level categories available in the Poweramp-style Library view.
 */
enum class LibraryCategory {
    Categories, // Main list of categories
    AllSongs,
    Folders,
    Albums,
    Artists,
    Genres
}

/**
 * UI State representation for the Library screen.
 */
sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    data class Success(
        val allTracks: List<AudioTrack>,
        val folders: List<MusicFolder>,
        val albums: List<MusicAlbum>,
        val artists: List<MusicArtist>,
        val genres: List<MusicGenre>,
        val currentCategory: LibraryCategory = LibraryCategory.Categories,
        val selectedFolder: MusicFolder? = null,
        val selectedAlbum: MusicAlbum? = null
    ) : LibraryUiState

    data class Error(val message: String) : LibraryUiState

    data object PermissionRequired : LibraryUiState
}

/**
 * ViewModel managing the discovery, grouping, and presentation of music categories.
 */
class LibraryViewModel(
    private val repository: AudioRepository = AudioRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    fun loadAudioFiles(context: Context) {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            try {
                val tracks = repository.getAudioTracks(context.applicationContext)
                val folders = repository.groupTracksByFolder(tracks)
                val albums = repository.groupTracksByAlbum(tracks)
                val artists = repository.groupTracksByArtist(tracks)
                val genres = repository.groupTracksByGenre(tracks)

                _uiState.value = LibraryUiState.Success(
                    allTracks = tracks,
                    folders = folders,
                    albums = albums,
                    artists = artists,
                    genres = genres,
                    currentCategory = LibraryCategory.Categories
                )
            } catch (e: SecurityException) {
                _uiState.value = LibraryUiState.PermissionRequired
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error(
                    e.localizedMessage ?: "An error occurred while loading audio files."
                )
            }
        }
    }

    fun navigateToCategory(category: LibraryCategory) {
        val currentState = _uiState.value
        if (currentState is LibraryUiState.Success) {
            _uiState.value = currentState.copy(
                currentCategory = category,
                selectedFolder = null,
                selectedAlbum = null
            )
        }
    }

    fun selectFolder(folder: MusicFolder?) {
        val currentState = _uiState.value
        if (currentState is LibraryUiState.Success) {
            _uiState.value = currentState.copy(selectedFolder = folder)
        }
    }

    fun selectAlbum(album: MusicAlbum?) {
        val currentState = _uiState.value
        if (currentState is LibraryUiState.Success) {
            _uiState.value = currentState.copy(selectedAlbum = album)
        }
    }

    fun handleBack(): Boolean {
        val currentState = _uiState.value
        if (currentState is LibraryUiState.Success) {
            if (currentState.selectedAlbum != null) {
                _uiState.value = currentState.copy(selectedAlbum = null)
                return true
            }
            if (currentState.selectedFolder != null) {
                _uiState.value = currentState.copy(selectedFolder = null)
                return true
            }
            if (currentState.currentCategory != LibraryCategory.Categories) {
                _uiState.value = currentState.copy(currentCategory = LibraryCategory.Categories)
                return true
            }
        }
        return false
    }

    fun setPermissionRequired() {
        _uiState.value = LibraryUiState.PermissionRequired
    }
}
