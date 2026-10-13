package com.zawa.client.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Base origin for the zawa.uk API + relative media paths. */
const val BASE = "https://www.zawa.uk"

/** Turn a possibly-relative media path into an absolute URL. */
fun absUrl(p: String?): String? {
    if (p.isNullOrBlank()) return null
    return if (p.startsWith("http://") || p.startsWith("https://")) p else BASE + p
}

@Serializable
data class Video(
    val id: String,
    val title: String,
    val category: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val views: Long = 0,
    val likes: Long = 0,
    @SerialName("fav_count") val favCount: Long = 0,
    @SerialName("exposures") val exposures: Long = 0,
    val tags: List<String> = emptyList(),
    val description: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("stream_url") val streamUrl: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("external_cover_url") val externalCoverUrl: String? = null,
    @SerialName("external_player_url") val externalPlayerUrl: String? = null,
    @SerialName("author_username") val authorUsername: String? = null,
    @SerialName("author_uid") val authorUid: Long? = null,
    @SerialName("author_verification_badge") val authorVerificationBadge: String? = null,
    @SerialName("author_verification_label") val authorVerificationLabel: String? = null,
    @SerialName("author_gravatar_url") val authorGravatarUrl: String? = null,
    @SerialName("recommendation_session") val recommendationSession: String? = null,
    @SerialName("recommendation_next_offset") val recommendationNextOffset: Long? = null
) {
    val playableUrl: String? get() = streamUrl ?: externalPlayerUrl
    val coverAbs: String? get() = absUrl(coverUrl) ?: externalCoverUrl
    val authorAvatarAbs: String? get() = absUrl(authorGravatarUrl)
}

@Serializable
data class Article(
    val id: String,
    val title: String,
    val summary: String? = null,
    val content: String? = null,
    val views: Long = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val status: String? = null,
    @SerialName("ip_location") val ipLocation: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("author_username") val authorUsername: String? = null,
    @SerialName("author_uid") val authorUid: Long? = null,
    @SerialName("author_ip_location") val authorIpLocation: String? = null,
    @SerialName("author_verification_badge") val authorVerificationBadge: String? = null,
    @SerialName("author_verification_label") val authorVerificationLabel: String? = null,
    @SerialName("author_gravatar_url") val authorGravatarUrl: String? = null
) {
    val coverAbs: String? get() = absUrl(coverUrl)
    val authorAvatarAbs: String? get() = absUrl(authorGravatarUrl)
}

@Serializable
data class Comment(
    val id: String? = null,
    val content: String? = null,
    val text: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("author_username") val authorUsername: String? = null,
    @SerialName("author_uid") val authorUid: Long? = null,
    @SerialName("author_gravatar_url") val authorGravatarUrl: String? = null,
    val likes: Long = 0,
    val replies: List<Comment> = emptyList()
) {
    val body: String get() = content ?: text ?: ""
    val avatarAbs: String? get() = absUrl(authorGravatarUrl)
}

@Serializable
data class CommentPage(
    val items: List<Comment> = emptyList(),
    val total: Long = 0,
    val limit: Long = 100,
    val offset: Long = 0
)

@Serializable
data class ZawaUser(
    val id: String? = null,
    val uid: Long,
    val username: String,
    val bio: String? = null,
    @SerialName("ip_location") val ipLocation: String? = null,
    @SerialName("account_status") val accountStatus: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("verification_badge") val verificationBadge: String? = null,
    @SerialName("verification_label") val verificationLabel: String? = null,
    @SerialName("follower_count") val followerCount: Long = 0,
    @SerialName("following_count") val followingCount: Long = 0,
    @SerialName("gravatar_url") val gravatarUrl: String? = null,
    @SerialName("is_following") val isFollowing: Boolean? = null
) {
    val avatarAbs: String? get() = absUrl(gravatarUrl)
}

@Serializable
data class UserProfile(
    val user: ZawaUser,
    val videos: List<Video> = emptyList()
)

@Serializable
data class TagHit(
    val name: String? = null,
    val tag: String? = null,
    @SerialName("count") val count: Long = 0,
    @SerialName("video_count") val videoCount: Long = 0,
    @SerialName("followed") val followed: Boolean? = null
) {
    val label: String get() = name ?: tag ?: ""
}
