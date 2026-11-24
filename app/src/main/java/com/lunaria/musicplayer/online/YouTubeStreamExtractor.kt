package com.lunaria.musicplayer.online

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException
import java.util.concurrent.TimeUnit

class OkHttpDownloader(private val client: OkHttpClient) : Downloader() {
    companion object {
        const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
    }

    @Throws(IOException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod().uppercase()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder().url(url)

        var hasUserAgent = false
        headers?.forEach { (headerName, headerValues) ->
            if (headerName.equals("User-Agent", ignoreCase = true)) {
                hasUserAgent = true
            }
            headerValues?.forEach { value ->
                requestBuilder.addHeader(headerName, value)
            }
        }

        if (!hasUserAgent) {
            requestBuilder.header("User-Agent", DEFAULT_USER_AGENT)
        }

        val requestBody = when {
            dataToSend != null -> dataToSend.toRequestBody(null)
            httpMethod == "POST" || httpMethod == "PUT" || httpMethod == "PATCH" -> ByteArray(0).toRequestBody(null)
            else -> null
        }

        requestBuilder.method(httpMethod, requestBody)

        val okResponse = client.newCall(requestBuilder.build()).execute()
        val responseBodyString = okResponse.body?.string().orEmpty()
        val responseCode = okResponse.code
        val responseMessage = okResponse.message
        val responseHeaders = okResponse.headers.toMultimap()
        val latestUrl = okResponse.request.url.toString()

        return Response(responseCode, responseMessage, responseHeaders, responseBodyString, latestUrl)
    }
}

object YouTubeStreamExtractor {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var isInitialized = false

    fun init() {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    NewPipe.init(OkHttpDownloader(client))
                    isInitialized = true
                }
            }
        }
    }

    suspend fun extractAudioStreamUrl(videoId: String): Result<String> = withContext(Dispatchers.IO) {
        init()
        var lastError: Throwable? = null

        for (attempt in 1..2) {
            try {
                val watchUrl = "https://www.youtube.com/watch?v=$videoId"
                val streamInfo = StreamInfo.getInfo(watchUrl)

                val audioStreams = streamInfo.audioStreams
                if (audioStreams.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("No audio streams available for this track"))
                }

                // Select highest bitrate stream, prioritizing M4A or WEBM_OPUS
                val bestStream = audioStreams
                    .filter { it.content?.isNotBlank() == true || it.url?.isNotBlank() == true }
                    .maxByOrNull { stream ->
                        var score = stream.bitrate
                        val formatName = stream.format?.name.orEmpty()
                        if (formatName.contains("M4A", ignoreCase = true)) {
                            score += 1000 // Preference for m4a
                        }
                        score
                    } ?: audioStreams.firstOrNull()

                val streamUrl = bestStream?.content?.ifBlank { null } ?: bestStream?.url?.ifBlank { null }
                if (streamUrl.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("Extracted stream URL is empty or unavailable"))
                }

                return@withContext Result.success(streamUrl)
            } catch (e: Throwable) {
                lastError = e
                val isReloadError = e.message?.contains("The page needs to be reloaded", ignoreCase = true) == true ||
                        e.cause?.message?.contains("The page needs to be reloaded", ignoreCase = true) == true ||
                        e is ContentNotAvailableException

                if (attempt == 1 && isReloadError) {
                    // Reset and reinitialize Downloader to clear any stale state
                    synchronized(this@YouTubeStreamExtractor) {
                        isInitialized = false
                        init()
                    }
                    continue
                } else {
                    break
                }
            }
        }

        Result.failure(lastError ?: Exception("Unknown error extracting audio stream for $videoId"))
    }
}
