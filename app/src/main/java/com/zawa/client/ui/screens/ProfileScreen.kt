package com.zawa.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zawa.client.data.Article
import com.zawa.client.data.UserProfile
import com.zawa.client.data.ZawaRepo
import com.zawa.client.ui.components.Avatar
import com.zawa.client.ui.components.ErrorState
import com.zawa.client.ui.components.LoadingBox
import com.zawa.client.ui.components.StatRow
import com.zawa.client.ui.components.VerificationChip
import com.zawa.client.ui.components.VideoThumb
import com.zawa.client.ui.components.countFmt

@Composable
fun ProfileScreen(uid: Long, onBack: () -> Unit, onVideo: (String) -> Unit, onArticle: (String) -> Unit) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var articles by remember { mutableStateOf<List<Article>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uid) {
        loading = true; error = null
        try {
            profile = ZawaRepo.user(uid)
            articles = runCatching { ZawaRepo.userArticles(uid) }.getOrNull() ?: emptyList()
        } catch (e: Exception) { error = e.message ?: "加载失败" }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface) }
            Text("主页", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
        when {
            loading -> LoadingBox()
            error != null -> ErrorState(error!!) { onBack() }
            else -> {
                val p = profile!!
                LazyColumn(Modifier.fillMaxSize()) {
                    item { ProfileHeader(p) }
                    val vids = p.videos
                    if (vids.isNotEmpty()) {
                        item {
                            Text("视频 · ${vids.size}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                        }
                    }
                    // use a column of 2-per-row rows to coexist with header in a single LazyColumn
                    val rows = vids.chunked(2)
                    items(rows) { rowVideos ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowVideos.forEach { v ->
                                Box(Modifier.weight(1f)) { MiniVideoCard(v) { onVideo(v.id) } }
                            }
                            if (rowVideos.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    if (articles.isNotEmpty()) {
                        item {
                            Text("专栏 · ${articles.size}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                        }
                        items(articles, key = { it.id }) { a ->
                            com.zawa.client.ui.screens.ArticleCard(a, onClick = { onArticle(a.id) }, onUser = { })
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(p: UserProfile) {
    val u = p.user
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(u.avatarAbs, 64)
            Spacer(Modifier.width(14.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(u.username, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.width(8.dp))
                    VerificationChip(u.verificationLabel)
                }
                Text("UID: ${u.uid}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (!u.bio.isNullOrBlank()) {
            Text(u.bio ?: "", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 12.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row {
            Stat(u.followerCount, "粉丝"); Spacer(Modifier.width(20.dp))
            Stat(u.followingCount, "关注")
        }
        Spacer(Modifier.height(6.dp))
        Text(
            buildString {
                u.ipLocation?.takeIf { it.isNotBlank() }?.let { append("IP属地: $it") }
                u.createdAt?.takeIf { it.isNotBlank() }?.let { if (isNotEmpty()) append(" · "); append("加入于 ${it.take(10)}") }
            },
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Stat(n: Long, label: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(countFmt(n), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.width(3.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MiniVideoCard(v: com.zawa.client.data.Video, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick).padding(5.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.5f)) {
            VideoThumb(v.coverAbs, Modifier.fillMaxSize())
        }
        Text(v.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp))
        Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            StatRow(v.views, v.likes, v.favCount)
        }
    }
}
