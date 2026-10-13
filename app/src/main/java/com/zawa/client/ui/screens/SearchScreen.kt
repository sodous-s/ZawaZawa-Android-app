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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zawa.client.data.TagHit
import com.zawa.client.data.Video
import com.zawa.client.data.ZawaRepo
import com.zawa.client.ui.components.LoadingBox
import com.zawa.client.ui.components.VideoThumb
import com.zawa.client.ui.components.countFmt
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(onTag: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results = remember { mutableStateListOf<TagHit>() }
    var searching by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("搜索标签，如 游戏 / 音乐") },
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        )
        // trigger search on edit (debounced-ish)
        LaunchedEffect(query) {
            if (query.isBlank()) { results.clear(); searching = false; return@LaunchedEffect }
            searching = true
            results.clear()
            try {
                results.addAll(ZawaRepo.searchTags(query.trim()))
            } catch (_: Exception) { }
            searching = false
        }
        when {
            query.isBlank() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("输入标签关键词以搜索视频", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            searching -> LoadingBox()
            results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("无结果", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results, key = { it.label }) { t ->
                    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().clickable { onTag(t.label) }) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("#${t.label}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            val cnt = if (t.videoCount > 0) t.videoCount else t.count
                            if (cnt > 0) Text("${countFmt(cnt)} 个视频", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TagScreen(tag: String, onVideo: (String) -> Unit, onBack: () -> Unit) {
    val items = remember { mutableStateListOf<Video>() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tag) {
        loading = true; error = null
        try {
            val list = ZawaRepo.videosByTag(tag, sort = "latest", limit = 24, offset = 0)
            items.clear(); items.addAll(list)
        } catch (e: Exception) { error = e.message ?: "加载失败" }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(12.dp)) {
            Text("标签 #$tag", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
        when {
            loading -> LoadingBox()
            error != null -> com.zawa.client.ui.components.ErrorState(error!!) { onBack() }
            items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("该标签暂无视频", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.id }) { v ->
                    Column(Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface).clickable { onVideo(v.id) }.padding(5.dp)) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1.5f)) { VideoThumb(v.coverAbs, Modifier.fillMaxSize()) }
                        Text(v.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp))
                        Text("${countFmt(v.views)} 播放", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}
