package com.zawa.client.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.zawa.client.data.Article
import com.zawa.client.data.BASE
import com.zawa.client.data.ZawaRepo
import com.zawa.client.ui.components.ErrorState
import com.zawa.client.ui.components.LoadingBox
import com.zawa.client.util.Markdown
import com.zawa.client.ui.components.Avatar
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ArticleReaderScreen(articleId: String, onBack: () -> Unit, onUser: (Long) -> Unit) {
    var article by remember { mutableStateOf<Article?>(null) }
    var comments by remember { mutableStateOf<List<com.zawa.client.data.Comment>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val dark = isSystemInDarkTheme()

    LaunchedEffect(articleId) {
        loading = true; error = null
        try {
            val a = ZawaRepo.article(articleId)
            article = a
            comments = runCatching { ZawaRepo.articleComments(articleId).items }.getOrNull() ?: emptyList()
        } catch (e: Exception) { error = e.message ?: "加载失败" }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface) }
            Text("专栏", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
        when {
            loading -> LoadingBox()
            error != null -> ErrorState(error!!) { onBack() }
            else -> {
                val a = article!!
                val html = Markdown.wrapHtml(a.title, Markdown.toHtml(a.content ?: a.summary ?: ""), dark)
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(a.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(Modifier.height(10.dp))
                            if (a.authorUid != null) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onUser(a.authorUid) }.padding(vertical = 4.dp)) {
                                    Avatar(a.authorAvatarAbs, 30)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(a.authorUsername ?: "", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                        Text("${a.authorIpLocation ?: ""} · ${fmtDateA(a.createdAt)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                                    settings.javaScriptEnabled = false
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    settings.defaultTextEncodingName = "utf-8"
                                    webViewClient = WebViewClient()
                                    loadDataWithBaseURL(BASE, html, "text/html", "utf-8", null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item { Text("评论 ${comments.size}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(16.dp)) }
                    if (comments.isEmpty()) {
                        item { Text("暂无评论", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp)) }
                    } else {
                        items(comments, key = { it.id ?: it.body }) { c -> CommentRow(c, onUser) }
                    }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }
}

private fun fmtDateA(s: String?): String =
    s?.take(10) ?: ""
