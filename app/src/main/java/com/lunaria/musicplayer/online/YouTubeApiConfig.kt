package com.lunaria.musicplayer.online

import androidx.core.text.HtmlCompat
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.lunaria.musicplayer.online.model.OnlineTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object YouTubeApiConfig {
    const val INNERTUBE_SEARCH_URL = "https://www.youtube.com/youtubei/v1/search?prettyPrint=false"
    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
}

class YouTubeSearchService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) {
    suspend fun searchMusic(query: String): Result<List<OnlineTrack>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        // Append 'audio' unless already specified to optimize search for audio/music tracks
        val searchQuery = if (trimmed.lowercase().contains("audio")) trimmed else "$trimmed audio"

        // Build InnerTube WEB client JSON payload
        val contextObj = JsonObject().apply {
            val clientObj = JsonObject().apply {
                addProperty("clientName", "WEB")
                addProperty("clientVersion", "2.20240501.00.00")
                addProperty("hl", "en")
                addProperty("gl", "US")
            }
            add("client", clientObj)
        }

        val requestPayload = JsonObject().apply {
            add("context", contextObj)
            addProperty("query", searchQuery)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestPayload.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(YouTubeApiConfig.INNERTUBE_SEARCH_URL)
            .post(requestBody)
            .header("Content-Type", "application/json")
            .header("User-Agent", YouTubeApiConfig.USER_AGENT)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP ${response.code}: ${response.message}")
                    )
                }

                val rootElement = try {
                    gson.fromJson(bodyString, JsonElement::class.java)
                } catch (e: Exception) {
                    return@withContext Result.failure(Exception("Failed to parse YouTube response: ${e.message}"))
                }

                val videoRenderers = mutableListOf<JsonObject>()
                extractVideoRenderers(rootElement, videoRenderers)

                val tracks = mutableListOf<OnlineTrack>()
                val seenVideoIds = mutableSetOf<String>()

                for (vr in videoRenderers) {
                    val videoId = vr.get("videoId")?.asString?.trim() ?: continue
                    if (videoId.isEmpty() || seenVideoIds.contains(videoId)) continue
                    seenVideoIds.add(videoId)

                    val rawTitle = extractTitle(vr) ?: continue
                    val rawChannel = extractChannel(vr) ?: "YouTube"

                    val cleanTitle = HtmlCompat.fromHtml(rawTitle, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                    val cleanChannel = HtmlCompat.fromHtml(rawChannel, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                    val thumbUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                    tracks.add(
                        OnlineTrack(
                            videoId = videoId,
                            title = cleanTitle,
                            channel = cleanChannel,
                            thumbnailUrl = thumbUrl
                        )
                    )

                    if (tracks.size >= 20) {
                        break
                    }
                }

                Result.success(tracks)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recursively traverses JSON elements to discover all videoRenderer objects.
     */
    private fun extractVideoRenderers(element: JsonElement?, outList: MutableList<JsonObject>, limit: Int = 30) {
        if (element == null || outList.size >= limit) return

        when {
            element.isJsonObject -> {
                val obj = element.asJsonObject
                if (obj.has("videoRenderer")) {
                    val vr = obj.getAsJsonObject("videoRenderer")
                    if (vr != null) {
                        outList.add(vr)
                        if (outList.size >= limit) return
                    }
                }
                for (entry in obj.entrySet()) {
                    extractVideoRenderers(entry.value, outList, limit)
                    if (outList.size >= limit) return
                }
            }
            element.isJsonArray -> {
                for (item in element.asJsonArray) {
                    extractVideoRenderers(item, outList, limit)
                    if (outList.size >= limit) return
                }
            }
        }
    }

    private fun extractTitle(vr: JsonObject): String? {
        val titleObj = vr.getAsJsonObject("title") ?: return null
        val runs = titleObj.getAsJsonArray("runs")
        if (runs != null && runs.size() > 0) {
            val text = runs.get(0)?.asJsonObject?.get("text")?.asString
            if (!text.isNullOrBlank()) return text
        }
        val simpleText = titleObj.get("simpleText")?.asString
        if (!simpleText.isNullOrBlank()) return simpleText

        val accessibility = vr.getAsJsonObject("accessibility")
        val label = accessibility?.getAsJsonObject("accessibilityData")?.get("label")?.asString
        if (!label.isNullOrBlank()) return label

        return null
    }

    private fun extractChannel(vr: JsonObject): String? {
        val ownerText = vr.getAsJsonObject("ownerText")
        val runs = ownerText?.getAsJsonArray("runs")
        if (runs != null && runs.size() > 0) {
            val text = runs.get(0)?.asJsonObject?.get("text")?.asString
            if (!text.isNullOrBlank()) return text
        }

        val shortByline = vr.getAsJsonObject("shortBylineText")
        val shortRuns = shortByline?.getAsJsonArray("runs")
        if (shortRuns != null && shortRuns.size() > 0) {
            val text = shortRuns.get(0)?.asJsonObject?.get("text")?.asString
            if (!text.isNullOrBlank()) return text
        }

        val longByline = vr.getAsJsonObject("longBylineText")
        val longRuns = longByline?.getAsJsonArray("runs")
        if (longRuns != null && longRuns.size() > 0) {
            val text = longRuns.get(0)?.asJsonObject?.get("text")?.asString
            if (!text.isNullOrBlank()) return text
        }

        return null
    }
}
