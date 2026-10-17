package com.zawa.client.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Singleton access to the public zawa.uk read API. */
object ZawaRepo {
    private val api = ZawaApi()

    suspend fun videos(category: String?, sort: String, limit: Int, offset: Int, session: String? = null): List<Video> =
        withContext(Dispatchers.IO) { api.fetchVideos(category, sort, limit, offset, session) }

    suspend fun videosByTag(tag: String, sort: String, limit: Int, offset: Int): List<Video> =
        withContext(Dispatchers.IO) { api.fetchVideosByTag(tag, sort, limit, offset) }

    suspend fun video(id: String): Video =
        withContext(Dispatchers.IO) { api.fetchVideo(id) }

    suspend fun videoComments(id: String, limit: Int = 100, offset: Int = 0): CommentPage =
        withContext(Dispatchers.IO) { api.fetchVideoComments(id, limit, offset) }

    suspend fun articles(limit: Int = 20): List<Article> =
        withContext(Dispatchers.IO) { api.fetchArticles(limit) }

    suspend fun article(id: String): Article =
        withContext(Dispatchers.IO) { api.fetchArticle(id) }

    suspend fun articleComments(id: String, limit: Int = 100, offset: Int = 0): CommentPage =
        withContext(Dispatchers.IO) { api.fetchArticleComments(id, limit, offset) }

    suspend fun user(uid: Long): UserProfile =
        withContext(Dispatchers.IO) { api.fetchUser(uid) }

    suspend fun userArticles(uid: Long): List<Article> =
        withContext(Dispatchers.IO) { api.fetchUserArticles(uid) }

    suspend fun searchTags(q: String): List<TagHit> =
        withContext(Dispatchers.IO) { api.searchTags(q) }

    // --- authenticated (cookie session) ---

    suspend fun me(cookieOverride: String? = null): ZawaUser =
        withContext(Dispatchers.IO) { api.fetchMe(cookieOverride) }

    suspend fun myVideos(): List<Video> =
        withContext(Dispatchers.IO) { api.fetchMyVideos() }

    suspend fun likeVideo(id: String): Boolean =
        withContext(Dispatchers.IO) { api.likeVideo(id) }

    suspend fun videoLiked(id: String): Boolean =
        withContext(Dispatchers.IO) { api.videoLiked(id) }

    suspend fun favoriteVideo(id: String): Boolean =
        withContext(Dispatchers.IO) { api.favoriteVideo(id) }

    suspend fun videoFavorited(id: String): Boolean =
        withContext(Dispatchers.IO) { api.videoFavorited(id) }

    suspend fun setFollow(uid: Long, follow: Boolean): Boolean =
        withContext(Dispatchers.IO) { api.setFollow(uid, follow) }

    suspend fun followStatus(uid: Long): Boolean =
        withContext(Dispatchers.IO) { api.followStatus(uid) }

    suspend fun logout() =
        withContext(Dispatchers.IO) { api.logout() }
}
