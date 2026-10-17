package com.zawa.client.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** Thrown when an authenticated endpoint returns 401 so the UI can route to login. */
class AuthRequiredException : IOException("AUTH_REQUIRED")

/**
 * Thin OkHttp client for the public zawa.uk read API plus the cookie-authenticated
 * write API (like / favorite / follow / me). All calls are synchronous; the
 * repository wraps them on Dispatchers.IO.
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

    private fun cookieHeader(override: String?): String? =
        (override ?: AuthStore.cookie()).takeIf { it.isNotBlank() }

    @Throws(IOException::class)
    private fun request(
        path: String,
        method: String = "GET",
        body: String? = null,
        cookieOverride: String? = null
    ): String {
        val b = Request.Builder()
            .url(path)
            .header("User-Agent", "ZawaZawa/1.0 Android")
            .header("Accept", "application/json")
        cookieHeader(cookieOverride)?.let { b.header("Cookie", it) }
        when (method) {
            "POST" -> b.post((body ?: "").toRequestBody("application/json".toMediaTypeOrNull()))
            "PUT" -> b.put((body ?: "").toRequestBody("application/json".toMediaTypeOrNull()))
            "DELETE" -> b.delete((body ?: "").toRequestBody("application/json".toMediaTypeOrNull()))
            else -> b.get()
        }
        client.newCall(b.build()).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (resp.code == 401) throw AuthRequiredException()
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            return text
        }
    }

    private fun get(path: String) = request(path)

    // --- public read API ---

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

    // --- authenticated API (cookie session) ---

    fun fetchMe(cookieOverride: String? = null): ZawaUser =
        json.decodeFromJsonElement(json.parseToJsonElement(request("$BASE/api/users/me", cookieOverride = cookieOverride)))

    fun fetchMyVideos(): List<Video> = decodeList(get("$BASE/api/users/me/videos"))

    fun likeVideo(id: String): Boolean =
        bool(request("$BASE/api/videos/${enc(id)}/like", method = "POST"), "liked")

    fun videoLiked(id: String): Boolean =
        bool(get("$BASE/api/videos/${enc(id)}/liked"), "liked")

    fun favoriteVideo(id: String): Boolean =
        bool(request("$BASE/api/videos/${enc(id)}/favorite", method = "POST"), "favorited")

    fun videoFavorited(id: String): Boolean =
        bool(get("$BASE/api/videos/${enc(id)}/favorited"), "favorited")

    fun setFollow(uid: Long, follow: Boolean): Boolean =
        bool(request("$BASE/api/users/$uid/follow", method = if (follow) "POST" else "DELETE"), "following", "is_following")

    fun followStatus(uid: Long): Boolean =
        bool(get("$BASE/api/users/$uid/follow-status"), "following", "is_following")

    fun logout() {
        runCatching { request("$BASE/api/auth/logout", method = "POST") }
    }

    // --- helpers ---

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun asArray(el: JsonElement): JsonArray? = when (el) {
        is JsonArray -> el
        is JsonObject -> listOf("items", "data", "videos", "articles", "list", "tags", "results")
            .firstNotNullOfOrNull { key -> el[key] as? JsonArray }
        else -> null
    }

    /** Read the first boolean-ish field among [keys] from a JSON object response. */
    private fun bool(body: String, vararg keys: String): Boolean {
        val obj = json.parseToJsonElement(body) as? JsonObject ?: return false
        for (k in keys) {
            val v = obj[k] as? JsonPrimitive ?: continue
            return v.booleanOrNull ?: (v.content == "1" || v.content.equals("true", ignoreCase = true))
        }
        return false
    }

    private inline fun <reified T> decodeList(body: String): List<T> {
        val el = json.parseToJsonElement(body)
        val arr = asArray(el) ?: return emptyList()
        return arr.mapNotNull { runCatching { json.decodeFromJsonElement<T>(it) }.getOrNull() }
    }
}
