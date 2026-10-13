package com.zawa.client.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Thin OkHttp client for the public zawa.uk read API.
 * All calls are synchronous; the repository wraps them on Dispatchers.IO.
 */
class ZawaApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Throws(IOException::class)
    private fun get(path: String): String {
        val req = Request.Builder()
            .url(path)
            .header("User-Agent", "ZawaZawa/1.0 Android")
            .header("Accept", "application/json")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            return body
        }
    }

    private fun videosPath(category: String?, sort: String, limit: Int, offset: Int, tag: String?, session: String?): String {
        val q = StringBuilder("$BASE/api/videos?limit=$limit&offset=$offset")
        if (!category.isNullOrBlank()) q.append("&category=").append(enc(category))
        q.append("&sort=").append(sort)
        if (!tag.isNullOrBlank()) q.append("&tag=").append(enc(tag))
        if (!session.isNullOrBlank()) q.append("&session=").append(enc(session))
        return q.toString()
    }

    fun fetchVideos(category: String?, sort: String, limit: Int, offset: Int, session: String? = null): List<Video> =
        decodeList(get(videosPath(category, sort, limit, offset, null, session)))

    fun fetchVideosByTag(tag: String, sort: String, limit: Int, offset: Int): List<Video> =
        decodeList(get(videosPath(null, sort, limit, offset, tag, null)))

    fun fetchVideo(id: String): Video =
        json.decodeFromJsonElement(json.parseToJsonElement(get("$BASE/api/videos/${enc(id)}")))

    fun fetchVideoComments(id: String, limit: Int = 100, offset: Int = 0): CommentPage =
        json.decodeFromJsonElement(json.parseToJsonElement(get("$BASE/api/videos/${enc(id)}/comments?limit=$limit&offset=$offset")))

    fun fetchArticles(limit: Int = 20): List<Article> =
        decodeList(get("$BASE/api/articles?limit=$limit"))

    fun fetchArticle(id: String): Article =
        json.decodeFromJsonElement(json.parseToJsonElement(get("$BASE/api/articles/${enc(id)}")))

    fun fetchArticleComments(id: String, limit: Int = 100, offset: Int = 0): CommentPage =
        json.decodeFromJsonElement(json.parseToJsonElement(get("$BASE/api/articles/${enc(id)}/comments?limit=$limit&offset=$offset")))

    fun fetchUser(uid: Long): UserProfile =
        json.decodeFromJsonElement(json.parseToJsonElement(get("$BASE/api/users/$uid")))

    fun fetchUserArticles(uid: Long): List<Article> =
        decodeList(get("$BASE/api/users/$uid/articles"))

    fun searchTags(q: String): List<TagHit> {
        if (q.isBlank()) return emptyList()
        val el = json.parseToJsonElement(get("$BASE/api/tags?q=${enc(q)}"))
        val arr = asArray(el) ?: return emptyList()
        return arr.mapNotNull { runCatching { json.decodeFromJsonElement<TagHit>(it) }.getOrNull() }
    }

    // --- helpers ---
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun asArray(el: JsonElement): JsonArray? = when (el) {
        is JsonArray -> el
        is JsonObject -> listOf("items", "data", "videos", "articles", "list", "tags", "results")
            .firstNotNullOfOrNull { key -> el[key] as? JsonArray }
        else -> null
    }

    private inline fun <reified T> decodeList(body: String): List<T> {
        val el = json.parseToJsonElement(body)
        val arr = asArray(el) ?: return emptyList()
        return arr.mapNotNull { runCatching { json.decodeFromJsonElement<T>(it) }.getOrNull() }
    }
}
