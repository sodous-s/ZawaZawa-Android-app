package com.zawa.client.ui.screens

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
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
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
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun VideoDetailScreen(videoId: String, onBack: () -> Unit, onUser: (Long) -> Unit) {
    val ctx = LocalContext.current
    var video by remember { mutableStateOf<Video?>(null) }
    var comments by remember { mutableStateOf<CommentPage?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(videoId) {
        loading = true; error = null
        try {
            video = ZawaRepo.video(videoId)
            comments = runCatching { ZawaRepo.videoComments(videoId) }.getOrNull()
        } catch (e: Exception) { error = e.message ?: "加载失败" }
        loading = false
    }

    val player = remember { ExoPlayer.Builder(ctx).build() }
    DisposableEffect(video?.playableUrl) {
        val url = video?.playableUrl
        if (!url.isNullOrBlank()) {
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.playWhenReady = true
        }
        onDispose { player.release() }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(color = Color.Black) {
            Box(Modifier.fillMaxWidth().height(220.dp)) {
                AndroidView(
                    factory = { PlayerView(it).apply { this.player = player; useController = true; setShowNextButton(false); setShowPreviousButton(false) } },
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
            }
        }
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
                                StatRow(v.views, v.likes, v.favCount)
                                Spacer(Modifier.width(10.dp))
                                v.category?.let { Text("· $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
