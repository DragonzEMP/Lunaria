package com.lunaria.musicplayer.data.coil

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream

/**
 * Custom Coil Fetcher that intelligently resolves and extracts embedded album artwork
 * from Android MediaStore URIs, virtual albumart paths, and raw audio files via
 * ContentResolver.loadThumbnail and MediaMetadataRetriever.
 */
class AudioArtworkFetcher(
    private val context: Context,
    private val data: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val contentResolver = context.contentResolver

        // 1. If it's an albumart URI, first try direct stream
        if (data.toString().contains("albumart")) {
            try {
                val inputStream = contentResolver.openInputStream(data)
                if (inputStream != null) {
                    return SourceResult(
                        source = ImageSource(inputStream.source().buffer(), context),
                        mimeType = null,
                        dataSource = DataSource.DISK
                    )
                }
            } catch (_: Exception) {
                // MediaStore albumart virtual table may fail on Android 10+
            }

            // Extract albumId from the URI and find a corresponding track URI
            val lastSegment = data.lastPathSegment?.toLongOrNull()
            if (lastSegment != null && lastSegment > 0) {
                val trackUri = findTrackUriForAlbum(contentResolver, lastSegment)
                if (trackUri != null) {
                    val result = extractArtworkFromTrackUri(contentResolver, trackUri)
                    if (result != null) return result
                }
            }
        }

        // 2. Direct track URI or generic audio URI
        return extractArtworkFromTrackUri(contentResolver, data)
    }

    private fun extractArtworkFromTrackUri(
        contentResolver: ContentResolver,
        trackUri: Uri
    ): FetchResult? {
        // A. On Android 10+ (API 29+), use ContentResolver.loadThumbnail
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val size = Size(512, 512)
                val bitmap = contentResolver.loadThumbnail(trackUri, size, null)
                return DrawableResult(
                    drawable = BitmapDrawable(context.resources, bitmap),
                    isSampled = false,
                    dataSource = DataSource.DISK
                )
            } catch (_: Exception) {
                // Fall back to MediaMetadataRetriever
            }
        }

        // B. MediaMetadataRetriever for embedded ID3/FLAC album art bytes
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, trackUri)
            val pictureBytes = retriever.embeddedPicture
            retriever.release()

            if (pictureBytes != null && pictureBytes.isNotEmpty()) {
                val inputStream = ByteArrayInputStream(pictureBytes)
                return SourceResult(
                    source = ImageSource(inputStream.source().buffer(), context),
                    mimeType = null,
                    dataSource = DataSource.DISK
                )
            }
        } catch (_: Exception) {
            // Cannot extract embedded picture
        }

        // C. Last attempt: open standard input stream directly
        try {
            val stream = contentResolver.openInputStream(trackUri)
            if (stream != null) {
                return SourceResult(
                    source = ImageSource(stream.source().buffer(), context),
                    mimeType = null,
                    dataSource = DataSource.DISK
                )
            }
        } catch (_: Exception) {
            // No direct stream
        }

        return null
    }

    private fun findTrackUriForAlbum(contentResolver: ContentResolver, albumId: Long): Uri? {
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.ALBUM_ID} = ? AND ${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val selectionArgs = arrayOf(albumId.toString())

        return try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val trackId = cursor.getLong(0)
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId)
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val uriStr = data.toString()
            if (uriStr.startsWith("content://") &&
                (uriStr.contains("audio") || uriStr.contains("media") || uriStr.contains("albumart"))
            ) {
                return AudioArtworkFetcher(context, data, options)
            }
            return null
        }
    }
}

