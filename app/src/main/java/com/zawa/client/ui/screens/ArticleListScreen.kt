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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.zawa.client.data.ZawaRepo
import com.zawa.client.ui.components.Avatar
import com.zawa.client.ui.components.ErrorState
import com.zawa.client.ui.components.LoadingBox
import com.zawa.client.ui.components.VerificationChip
import com.zawa.client.ui.components.countFmt

@Composable
fun ArticleListScreen(onArticle: (String) -> Unit, onUser: (Long) -> Unit) {
    val items = remember { mutableStateListOf<Article>() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true; error = null
        try {
            val list = ZawaRepo.articles(limit = 40)
            items.clear(); items.addAll(list)
        } catch (e: Exception) { error = e.message ?: "加载失败" }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            loading -> LoadingBox()
            error != null -> ErrorState(error!!) { }
            items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无专栏", color = MaterialTheme.colorScheme.onBackground)
            }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id }) { a ->
                    ArticleCard(a, onClick = { onArticle(a.id) }, onUser = { onUser(it) })
                }
            }
        }
    }
}

@Composable
fun ArticleCard(a: Article, onClick: () -> Unit, onUser: (Long) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(10.dp)) {
            a.coverAbs?.let { url ->
                Box(
                    Modifier.size(96.dp, 64.dp).clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    com.zawa.client.ui.components.VideoThumb(url, Modifier.fillMaxSize(), corner = 8)
                }
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(a.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                if (!a.summary.isNullOrBlank()) {
                    Text(a.summary ?: "", maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (a.authorUid != null) {
                        Avatar(a.authorAvatarAbs, 16, Modifier.clickable { onUser(a.authorUid) })
                        Spacer(Modifier.width(4.dp))
                        Text(a.authorUsername ?: "", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clickable { onUser(a.authorUid) })
                        Spacer(Modifier.width(4.dp))
                        VerificationChip(a.authorVerificationLabel)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("${countFmt(a.views)} 阅读", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
