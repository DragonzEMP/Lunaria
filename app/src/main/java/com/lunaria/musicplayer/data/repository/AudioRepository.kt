package com.lunaria.musicplayer.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.lunaria.musicplayer.domain.model.AudioTrack
import com.lunaria.musicplayer.domain.model.MusicAlbum
import com.lunaria.musicplayer.domain.model.MusicArtist
import com.lunaria.musicplayer.domain.model.MusicFolder
import com.lunaria.musicplayer.domain.model.MusicGenre
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository responsible for querying audio media and organizing files into categories.
 */
class AudioRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Queries local music files from MediaStore.Audio.Media on the IO dispatcher.
     */
    suspend fun getAudioTracks(context: Context): List<AudioTrack> = withContext(ioDispatcher) {
        val tracks = mutableListOf<AudioTrack>()

        val projectionList = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.MIME_TYPE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projectionList.add(MediaStore.Audio.Media.RELATIVE_PATH)
        }

        val projection = projectionList.toTypedArray()
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val trackNumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val yearColumn = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val mimeTypeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val relativePathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val rawTitle = cursor.getString(titleColumn)
                    val rawArtist = cursor.getString(artistColumn)
                    val rawAlbum = cursor.getString(albumColumn)
                    val duration = cursor.getLong(durationColumn)
                    val albumId = cursor.getLong(albumIdColumn)

                    val data = if (dataColumn >= 0) cursor.getString(dataColumn) else null
                    val rawTrackNum = if (trackNumColumn >= 0) cursor.getInt(trackNumColumn) else 1
                    val year = if (yearColumn >= 0) cursor.getInt(yearColumn) else 0
                    val mimeType = if (mimeTypeColumn >= 0) cursor.getString(mimeTypeColumn) else null
                    val relativePath = if (relativePathColumn >= 0) cursor.getString(relativePathColumn) else null

                    // Normalize unknown metadata strings
                    val title = if (rawTitle.isNullOrBlank() || rawTitle == "<unknown>") {
                        "Track #$id"
                    } else {
                        rawTitle
                    }

                    val artist = if (rawArtist.isNullOrBlank() || rawArtist == "<unknown>") {
                        "Unknown Artist"
                    } else {
                        rawArtist
                    }

                    val album = if (rawAlbum.isNullOrBlank() || rawAlbum == "<unknown>") {
                        "Unknown Album"
                    } else {
                        rawAlbum
                    }

                    val trackUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val albumArtUri = if (albumId >= 0) {
                        ContentUris.withAppendedId(albumArtBaseUri, albumId)
                    } else {
                        null
                    }

                    val (folderName, folderPath) = resolveFolderInfo(data, relativePath, album)
                    val format = resolveFormat(data, mimeType)

                    // Track number can sometimes encode disc number e.g. 1001 -> disc 1 track 1
                    val trackNumber = if (rawTrackNum > 0) rawTrackNum % 1000 else 1

                    tracks.add(
                        AudioTrack(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            uri = trackUri,
                            albumArtUri = albumArtUri,
                            folderName = folderName,
                            folderPath = folderPath,
                            trackNumber = trackNumber,
                            format = format,
                            bitrate = "320 kbps",
                            sampleRate = "44.1 kHz",
                            year = year
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        tracks
    }

    /**
     * Groups a collection of AudioTracks by their parent directory/folder.
     */
    fun groupTracksByFolder(tracks: List<AudioTrack>): List<MusicFolder> {
        return tracks
            .groupBy { it.folderPath.ifBlank { it.folderName } }
            .map { (path, folderTracks) ->
                val firstTrack = folderTracks.first()
                val folderName = firstTrack.folderName
                val artUri = folderTracks.firstOrNull { it.albumArtUri != null }?.albumArtUri
                MusicFolder(
                    name = folderName,
                    path = path,
                    tracks = folderTracks.sortedBy { it.title.lowercase() },
                    representativeArtUri = artUri
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Groups a collection of AudioTracks by their Album.
     */
    fun groupTracksByAlbum(tracks: List<AudioTrack>): List<MusicAlbum> {
        return tracks
            .groupBy { it.album }
            .entries
            .mapIndexed { index, entry ->
                val albumName = entry.key
                val albumTracks = entry.value
                val first = albumTracks.first()
                val artUri = albumTracks.firstOrNull { it.albumArtUri != null }?.albumArtUri
                MusicAlbum(
                    id = index.toLong(),
                    name = albumName,
                    artist = first.artist,
                    tracks = albumTracks.sortedBy { it.trackNumber },
                    albumArtUri = artUri,
                    year = first.year
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Groups a collection of AudioTracks by Artist.
     */
    fun groupTracksByArtist(tracks: List<AudioTrack>): List<MusicArtist> {
        return tracks
            .groupBy { it.artist }
            .map { (artistName, artistTracks) ->
                val artUri = artistTracks.firstOrNull { it.albumArtUri != null }?.albumArtUri
                MusicArtist(
                    name = artistName,
                    tracks = artistTracks.sortedBy { it.title.lowercase() },
                    representativeArtUri = artUri
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Groups a collection of AudioTracks by Genre.
     */
    fun groupTracksByGenre(tracks: List<AudioTrack>): List<MusicGenre> {
        // Fallback default categorization if genre tags are not in media store
        val defaultGenres = listOf("Electronic", "Synthwave", "Ambient", "Rock", "Pop", "Classical")
        return defaultGenres.map { genreName ->
            MusicGenre(
                name = genreName,
                tracks = tracks.filter { it.title.contains(genreName, ignoreCase = true) || it.album.contains(genreName, ignoreCase = true) }
            )
        }.filter { it.trackCount > 0 }.ifEmpty {
            listOf(MusicGenre("General", tracks))
        }
    }

    private fun resolveFormat(data: String?, mimeType: String?): String {
        if (!data.isNullOrBlank()) {
            val ext = data.substringAfterLast('.', "").uppercase()
            if (ext.isNotBlank() && ext.length <= 5) return ext
        }
        if (!mimeType.isNullOrBlank()) {
            return when {
                mimeType.contains("flac", ignoreCase = true) -> "FLAC"
                mimeType.contains("wav", ignoreCase = true) -> "WAV"
                mimeType.contains("mp4", ignoreCase = true) || mimeType.contains("m4a", ignoreCase = true) -> "M4A"
                mimeType.contains("ogg", ignoreCase = true) -> "OGG"
                else -> "MP3"
            }
        }
        return "MP3"
    }

    private fun resolveFolderInfo(
        data: String?,
        relativePath: String?,
        fallbackAlbum: String?
    ): Pair<String, String> {
        if (!relativePath.isNullOrBlank()) {
            val cleaned = relativePath.trim('/', '\\').replace('\\', '/')
            val segments = cleaned.split('/')
            val folderName = segments.lastOrNull()?.ifBlank { null } ?: "Music"
            return Pair(folderName, cleaned)
        }

        if (!data.isNullOrBlank()) {
            try {
                val file = File(data)
                val parent = file.parentFile
                if (parent != null && parent.name.isNotBlank()) {
                    return Pair(parent.name, parent.absolutePath)
                }
            } catch (_: Exception) {}
        }

        val fallback = if (!fallbackAlbum.isNullOrBlank() && fallbackAlbum != "<unknown>") {
            fallbackAlbum
        } else {
            "Internal Music"
        }
        return Pair(fallback, fallback)
    }
}
