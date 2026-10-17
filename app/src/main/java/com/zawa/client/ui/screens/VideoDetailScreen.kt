package com.zawa.client.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.zawa.client.data.AuthStore
import com.zawa.client.data.Comment
import com.zawa.client.data.CommentPage
import com.zawa.client.data.Video
import com.zawa.client.data.ZawaRepo
import com.zawa.client.ui.components.Avatar
import com.zawa.client.ui.components.ErrorState
import com.zawa.client.ui.components.LoadingBox
import com.zawa.client.ui.components.StatRow
import com.zawa.client.ui.components.VerificationChip
import com.zawa.client.ui.components.countFmt
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun VideoDetailScreen(
    videoId: String,
    onBack: () -> Unit,
    onUser: (Long) -> Unit,
    onLogin: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var video by remember { mutableStateOf<Video?>(null) }
    var comments by remember { mutableStateOf<CommentPage?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var liked by remember { mutableStateOf(false) }
    var favorited by remember { mutableStateOf(false) }
    var likeCount by remember { mutableStateOf(0L) }
    var favCount by remember { mutableStateOf(0L) }
    var fullscreen by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(videoId) {
        loading = true; error = null
        try {
            video = ZawaRepo.video(videoId)
            comments = runCatching { ZawaRepo.videoComments(videoId) }.getOrNull()
        } catch (e: Exception) { error = e.message ?: "加载失败" }
        loading = false
    }

    // Seed counters from the loaded video and resolve the viewer's like/favorite state.
    LaunchedEffect(video?.id, AuthStore.user?.uid) {
        val v = video ?: return@LaunchedEffect
        likeCount = v.likes
        favCount = v.favCount
        if (!AuthStore.isLoggedIn) {
            liked = false; favorited = false
            return@LaunchedEffect
        }
        runCatching { ZawaRepo.videoLiked(v.id) }.getOrNull()?.let { liked = it }
        runCatching { ZawaRepo.videoFavorited(v.id) }.getOrNull()?.let { favorited = it }
    }

    val player = remember { ExoPlayer.Builder(ctx).build() }
    // Release the player only when the screen leaves composition. Previously the
    // release was tied to the media-URL key, so as soon as the video finished
    // loading (url: null -> "...mp4") the old effect disposed and released the
    // player, then the new effect prepared media on a released player — leaving
    // the controller stuck at 0:00.
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }
    LaunchedEffect(video?.playableUrl) {
        val url = video?.playableUrl
        if (!url.isNullOrBlank()) {
            playerError = null
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.playWhenReady = true
        }
    }
    // Surface buffering / decode errors so "卡" 与 "bug" 能一眼区分。
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) playerError = null
            }

            override fun onPlayerError(e: PlaybackException) {
                buffering = false
                playerError = e.errorCodeName + (e.message?.let { " · $it" } ?: "")
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    val activity = ctx.findActivity()
    BackHandler(enabled = fullscreen) { fullscreen = false }
    LaunchedEffect(fullscreen) {
        val a = activity ?: return@LaunchedEffect
        if (fullscreen) {
            a.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            setSystemBarsVisible(a, false)
        } else {
            a.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            setSystemBarsVisible(a, true)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.let { setSystemBarsVisible(it, true) }
        }
    }

    fun toggleLike() {
        val v = video ?: return
        if (!AuthStore.isLoggedIn) { onLogin(); return }
        val prevLiked = liked; val prevCount = likeCount
        val target = !prevLiked
        liked = target
        likeCount = (prevCount + if (target) 1 else -1).coerceAtLeast(0)
        scope.launch {
            val res = runCatching { ZawaRepo.likeVideo(v.id) }.getOrNull()
            if (res == null) { liked = prevLiked; likeCount = prevCount }
            else if (res != target) {
                liked = res
                likeCount = (prevCount + if (res) 1 else -1).coerceAtLeast(0)
            }
        }
    }

    fun toggleFavorite() {
        val v = video ?: return
        if (!AuthStore.isLoggedIn) { onLogin(); return }
        val prevFav = favorited; val prevCount = favCount
        val target = !prevFav
        favorited = target
        favCount = (prevCount + if (target) 1 else -1).coerceAtLeast(0)
        scope.launch {
            val res = runCatching { ZawaRepo.favoriteVideo(v.id) }.getOrNull()
            if (res == null) { favorited = prevFav; favCount = prevCount }
            else if (res != target) {
                favorited = res
                favCount = (prevCount + if (res) 1 else -1).coerceAtLeast(0)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(
            color = Color.Black,
            modifier = if (fullscreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(220.dp)
        ) {
            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { PlayerView(it).apply { this.player = player; useController = true; setShowNextButton(false); setShowPreviousButton(false) } },
                    modifier = Modifier.fillMaxSize()
                )
                when {
                    playerError != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "播放失败\n$playerError",
                            color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                    buffering -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                if (!fullscreen) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                }
                IconButton(
                    onClick = { fullscreen = !fullscreen },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                ) {
                    Icon(
                        if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (fullscreen) "退出全屏" else "全屏",
                        tint = Color.White
                    )
                }
            }
        }

        if (!fullscreen) {
            when {
                loading -> LoadingBox()
                error != null -> ErrorState(error!!) { onBack() }
                else -> {
                    val v = video!!
                    LazyColumn(Modifier.fillMaxSize()) {
                        item {
                            Column(Modifier.padding(14.dp)) {
                                Text(v.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatRow(v.views, likeCount, favCount)
                                    Spacer(Modifier.width(10.dp))
                                    v.category?.let { Text("· $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ActionButton(
                                        icon = Icons.Default.ThumbUp,
                                        label = if (likeCount > 0) countFmt(likeCount) else "点赞",
                                        active = liked,
                                        onClick = { toggleLike() }
                                    )
                                    ActionButton(
                                        icon = if (favorited) Icons.Default.Star else Icons.Default.StarBorder,
                                        label = if (favCount > 0) countFmt(favCount) else "收藏",
                                        active = favorited,
                                        onClick = { toggleFavorite() }
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                if (v.authorUid != null) {
                                    Row(
                                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .clickable { onUser(v.authorUid) }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Avatar(v.authorAvatarAbs, 40)
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(v.authorUsername ?: "", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                                Spacer(Modifier.width(6.dp))
                                                VerificationChip(v.authorVerificationLabel)
                                            }
                                            Text("${countFmt(v.views)} 次观看 · ${fmtDate(v.createdAt)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                v.description?.takeIf { it.isNotBlank() }?.let {
                                    Spacer(Modifier.height(12.dp))
                                    Text(it.trim(), fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, lineHeight = 22.sp)
                                }
                                if (v.tags.isNotEmpty()) {
                                    Spacer(Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        v.tags.take(8).forEach { t ->
                                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
                                                Text("#$t", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item { Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 1.dp) }
                        item {
                            Text("评论 ${comments?.total ?: 0}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onBackground)
                        }
                        val list = comments?.items ?: emptyList()
                        if (list.isEmpty()) {
                            item { Text("暂无评论", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(14.dp)) }
                        } else {
                            items(list, key = { it.id ?: it.body }) { c -> CommentRow(c, onUser) }
                        }
                        item { Spacer(Modifier.height(40.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(18.dp), tint = tint)
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 13.sp, color = tint, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun CommentRow(c: Comment, onUser: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Avatar(c.avatarAbs, 32)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.authorUsername ?: "匿名", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                if (c.authorUid != null) Spacer(Modifier.width(6.dp)) else Unit
                if (c.likes > 0) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Favorite, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(" ${countFmt(c.likes)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(c.body, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 2.dp))
            if (c.createdAt != null) Text(fmtDate(c.createdAt), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

private fun fmtDate(s: String?): String {
    if (s.isNullOrBlank()) return ""
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).parse(s)
            ?.let { SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(it) }
    }.getOrNull() ?: s.take(10)
}

private fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

private fun setSystemBarsVisible(activity: Activity, visible: Boolean) {
    val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
    if (visible) {
        controller.show(WindowInsetsCompat.Type.systemBars())
    } else {
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
