package com.zawa.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zawa.client.data.Video
import com.zawa.client.data.ZawaRepo
import com.zawa.client.ui.Routes
import com.zawa.client.ui.components.Avatar
import com.zawa.client.ui.components.LoadingBox
import com.zawa.client.ui.components.StatRow
import com.zawa.client.ui.components.VideoThumb
import com.zawa.client.ui.components.VerificationChip
import com.zawa.client.ui.components.countFmt
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

private data class Category(val key: String, val label: String)
private val CATEGORIES = listOf(
    Category("All", "全部"),
    Category("Tech", "科技"),
    Category("Music", "音乐"),
    Category("Game", "游戏"),
    Category("Entertainment", "娱乐"),
    Category("Life", "生活"),
    Category("Sport", "体育"),
    Category("Food", "美食"),
    Category("Travel", "旅行"),
    Category("Auto", "汽车"),
    Category("Art", "艺术")
)

@Composable
fun VideoFeedScreen(onVideo: (String) -> Unit, onUser: (Long) -> Unit) {
    val scope = rememberCoroutineScope()
    val items = remember { mutableStateListOf<Video>() }
    var sort by remember { mutableStateOf("recommend") }
    var category by remember { mutableStateOf("All") }
    var offset by remember { mutableStateOf(0) }
    var session by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var initialized by remember { mutableStateOf(false) }

    suspend fun loadFirst() {
        loading = true; error = null
        try {
            val list = ZawaRepo.videos(category, sort, limit = 24, offset = 0, session = null)
            items.clear(); items.addAll(list)
            offset = 24
            session = list.firstOrNull { !it.recommendationSession.isNullOrBlank() }?.recommendationSession
            hasMore = list.size >= 24
        } catch (e: Exception) { error = e.message ?: "加载失败" }
        loading = false
    }

    suspend fun loadMore() {
        if (loadingMore || !hasMore) return
        loadingMore = true
        try {
            val nextOffset = if (sort == "recommend" && session != null) {
                items.lastOrNull { it.recommendationNextOffset != null }?.recommendationNextOffset?.toInt() ?: offset
            } else offset
            val list = ZawaRepo.videos(category, sort, limit = 24, offset = nextOffset, session = session)
            items.addAll(list)
            offset = nextOffset + list.size
            hasMore = list.size >= 24
        } catch (_: Exception) { }
        loadingMore = false
    }

    fun refresh(cat: String, s: String) {
        category = cat; sort = s; offset = 0; session = null; hasMore = true; initialized = false
        scope.launch { loadFirst(); initialized = true }
    }

    LaunchedEffect(Unit) {
        if (!initialized) { loadFirst(); initialized = true }
    }

    val gridState = rememberLazyGridState()
    LaunchedEffect(gridState, items.size) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val total = info.totalItemsCount
            if (total == 0) -1 else info.visibleItemsInfo.lastOrNull()?.index ?: -1
        }.distinctUntilChanged().filter { it >= 0 && it >= items.size - 6 }
            .collect { if (items.isNotEmpty() && !loading) loadMore() }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            LazyColumn(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("recommend" to "推荐", "latest" to "最新").forEach { (k, l) ->
                            FilterChip(
                                selected = sort == k,
                                onClick = { refresh(category, k) },
                                label = { Text(l) }
                            )
                        }
                    }
                }
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CATEGORIES.forEach { c ->
                            FilterChip(
                                selected = category == c.key,
                                onClick = { refresh(c.key, sort) },
                                label = { Text(c.label, maxLines = 1) }
                            )
                        }
                    }
                }
            }
        }

        when {
            loading && items.isEmpty() -> LoadingBox()
            error != null && items.isEmpty() -> com.zawa.client.ui.components.ErrorState(error!!) { scope.launch { loadFirst() } }
            else -> LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.id }) { v ->
                    VideoCard(v, onClick = { onVideo(v.id) }, onUser = { onUser(it) })
                }
                if (loadingMore) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoCard(v: Video, onClick: () -> Unit, onUser: (Long) -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.5f)) {
            VideoThumb(v.coverAbs, Modifier.fillMaxSize())
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
            ) {
                Text(
                    if (v.durationSecLabel().isBlank()) "${countFmt(v.views)} 播放" else v.durationSecLabel(),
                    color = Color.White, fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
        Text(
            v.title,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp)
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp, start = 2.dp, end = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (v.authorUid != null) {
                Avatar(v.authorAvatarAbs, 18, Modifier.clickable { onUser(v.authorUid) })
                Spacer(Modifier.width(4.dp))
                Text(
                    v.authorUsername ?: "",
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).clickable { v.authorUid?.let(onUser) }
                )
            } else { Spacer(Modifier.weight(1f)) }
            StatRow(v.views, v.likes, v.favCount)
        }
    }
}

private fun Video.durationSecLabel(): String = "" // stream length unknown from API; reserved
