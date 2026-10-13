package com.zawa.client.ui

object Routes {
    const val VIDEOS = "videos"
    const val ARTICLES = "articles"
    const val SEARCH = "search"

    const val VIDEO = "video/{id}"
    fun video(id: String) = "video/$id"

    const val ARTICLE = "article/{id}"
    fun article(id: String) = "article/$id"

    const val USER = "user/{uid}"
    fun user(uid: Long) = "user/$uid"

    const val TAG = "tag/{tag}"
    fun tag(tag: String) = "tag/${java.net.URLEncoder.encode(tag, "UTF-8")}"
}
