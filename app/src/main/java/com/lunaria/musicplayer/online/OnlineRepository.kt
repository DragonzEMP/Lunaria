package com.lunaria.musicplayer.online

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lunaria.musicplayer.online.model.OnlinePlaylist
import com.lunaria.musicplayer.online.model.OnlineTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

val Context.onlineDataStore: DataStore<Preferences> by preferencesDataStore(name = "lunaria_online_prefs")

class OnlineRepository(private val context: Context) {

    private val gson = Gson()

    private val keySearchHistory = stringPreferencesKey("online_search_history")
    private val keySavedTracks = stringPreferencesKey("online_saved_tracks")
    private val keyPlaylists = stringPreferencesKey("online_playlists")

    private val listStringType = object : TypeToken<List<String>>() {}.type
    private val trackListType = object : TypeToken<List<OnlineTrack>>() {}.type
    private val playlistListType = object : TypeToken<List<OnlinePlaylist>>() {}.type

    // MARK: - Search History
    val searchHistory: Flow<List<String>> = context.onlineDataStore.data.map { prefs ->
        val json = prefs[keySearchHistory] ?: return@map emptyList()
        try {
            gson.fromJson<List<String>>(json, listStringType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        context.onlineDataStore.edit { prefs ->
            val json = prefs[keySearchHistory]
            val currentList = try {
                if (json != null) gson.fromJson<List<String>>(json, listStringType)?.toMutableList() ?: mutableListOf()
                else mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }

            // Remove existing duplicate and insert at beginning
            currentList.removeAll { it.equals(trimmed, ignoreCase = true) }
            currentList.add(0, trimmed)

            // Auto-trim to max 100 entries
            val trimmedList = if (currentList.size > 100) currentList.take(100) else currentList
            prefs[keySearchHistory] = gson.toJson(trimmedList)
        }
    }

    suspend fun removeSearchQuery(query: String) {
        context.onlineDataStore.edit { prefs ->
            val json = prefs[keySearchHistory] ?: return@edit
            val currentList = try {
                gson.fromJson<List<String>>(json, listStringType)?.toMutableList() ?: return@edit
            } catch (e: Exception) {
                return@edit
            }
            if (currentList.removeAll { it.equals(query, ignoreCase = true) }) {
                prefs[keySearchHistory] = gson.toJson(currentList)
            }
        }
    }

    suspend fun clearSearchHistory() {
        context.onlineDataStore.edit { prefs ->
            prefs.remove(keySearchHistory)
        }
    }

    // MARK: - Saved Songs
    val savedTracks: Flow<List<OnlineTrack>> = context.onlineDataStore.data.map { prefs ->
        val json = prefs[keySavedTracks] ?: return@map emptyList()
        try {
            gson.fromJson<List<OnlineTrack>>(json, trackListType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveTrack(track: OnlineTrack) {
        context.onlineDataStore.edit { prefs ->
            val json = prefs[keySavedTracks]
            val currentList = try {
                if (json != null) gson.fromJson<List<OnlineTrack>>(json, trackListType)?.toMutableList() ?: mutableListOf()
                else mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }

            currentList.removeAll { it.videoId == track.videoId }
            currentList.add(0, track)
            prefs[keySavedTracks] = gson.toJson(currentList)
        }
    }

    suspend fun unsaveTrack(videoId: String) {
        context.onlineDataStore.edit { prefs ->
            val json = prefs[keySavedTracks] ?: return@edit
            val currentList = try {
                gson.fromJson<List<OnlineTrack>>(json, trackListType)?.toMutableList() ?: return@edit
            } catch (e: Exception) {
                return@edit
            }
            if (currentList.removeAll { it.videoId == videoId }) {
                prefs[keySavedTracks] = gson.toJson(currentList)
            }
        }
    }

    suspend fun toggleSaveTrack(track: OnlineTrack): Boolean {
        var isNowSaved = false
        context.onlineDataStore.edit { prefs ->
            val json = prefs[keySavedTracks]
            val currentList = try {
                if (json != null) gson.fromJson<List<OnlineTrack>>(json, trackListType)?.toMutableList() ?: mutableListOf()
                else mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }

            val exists = currentList.any { it.videoId == track.videoId }
            if (exists) {
                currentList.removeAll { it.videoId == track.videoId }
                isNowSaved = false
            } else {
                currentList.add(0, track)
                isNowSaved = true
            }
            prefs[keySavedTracks] = gson.toJson(currentList)
        }
        return isNowSaved
    }

    // MARK: - Custom Playlists
    val playlists: Flow<List<OnlinePlaylist>> = context.onlineDataStore.data.map { prefs ->
        val json = prefs[keyPlaylists] ?: return@map emptyList()
        try {
            gson.fromJson<List<OnlinePlaylist>>(json, playlistListType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createPlaylist(name: String): String {
        val trimmed = name.trim()
        val newId = UUID.randomUUID().toString()
        context.onlineDataStore.edit { prefs ->
            val json = prefs[keyPlaylists]
            val currentList = try {
                if (json != null) gson.fromJson<List<OnlinePlaylist>>(json, playlistListType)?.toMutableList() ?: mutableListOf()
                else mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }

            currentList.add(OnlinePlaylist(id = newId, name = trimmed.ifEmpty { "New Playlist" }, tracks = emptyList()))
            prefs[keyPlaylists] = gson.toJson(currentList)
        }
        return newId
    }

    suspend fun deletePlaylist(playlistId: String) {
        context.onlineDataStore.edit { prefs ->
            val json = prefs[keyPlaylists] ?: return@edit
            val currentList = try {
                gson.fromJson<List<OnlinePlaylist>>(json, playlistListType)?.toMutableList() ?: return@edit
            } catch (e: Exception) {
                return@edit
            }
            if (currentList.removeAll { it.id == playlistId }) {
                prefs[keyPlaylists] = gson.toJson(currentList)
            }
        }
    }

    suspend fun addTrackToPlaylist(playlistId: String, track: OnlineTrack) {
        context.onlineDataStore.edit { prefs ->
            val json = prefs[keyPlaylists] ?: return@edit
            val currentList = try {
                gson.fromJson<List<OnlinePlaylist>>(json, playlistListType)?.toMutableList() ?: return@edit
            } catch (e: Exception) {
                return@edit
            }

            val index = currentList.indexOfFirst { it.id == playlistId }
            if (index != -1) {
                val target = currentList[index]
                // Prevent duplicate in same playlist
                val updatedTracks = target.tracks.filter { it.videoId != track.videoId }.toMutableList()
                updatedTracks.add(track)
                currentList[index] = target.copy(tracks = updatedTracks)
                prefs[keyPlaylists] = gson.toJson(currentList)
            }
        }
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, videoId: String) {
        context.onlineDataStore.edit { prefs ->
            val json = prefs[keyPlaylists] ?: return@edit
            val currentList = try {
                gson.fromJson<List<OnlinePlaylist>>(json, playlistListType)?.toMutableList() ?: return@edit
            } catch (e: Exception) {
                return@edit
            }

            val index = currentList.indexOfFirst { it.id == playlistId }
            if (index != -1) {
                val target = currentList[index]
                val updatedTracks = target.tracks.filter { it.videoId != videoId }
                currentList[index] = target.copy(tracks = updatedTracks)
                prefs[keyPlaylists] = gson.toJson(currentList)
            }
        }
    }
}
