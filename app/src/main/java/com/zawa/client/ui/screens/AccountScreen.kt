package com.zawa.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zawa.client.data.AuthStore
import com.zawa.client.data.Video
import com.zawa.client.data.ZawaRepo
import com.zawa.client.ui.components.Avatar
import com.zawa.client.ui.components.VerificationChip
import com.zawa.client.ui.components.countFmt
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    onLogin: () -> Unit,
    onVideo: (String) -> Unit,
    onUpload: () -> Unit
) {
    val me = AuthStore.user
    val scope = rememberCoroutineScope()
    val myVideos = remember { mutableStateListOf<Video>() }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(me?.uid) {
        if (me != null) {
            loading = true
            myVideos.clear()
            runCatching { ZawaRepo.myVideos() }.getOrNull()?.let { myVideos.addAll(it) }
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("我的", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        if (me == null) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("登录后可点赞、收藏、关注和上传视频", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onLogin) { Text("登录 / 注册") }
            }
            return@Column
        }

        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(me.avatarAbs, 64)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(me.username, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Spacer(Modifier.width(8.dp))
                                VerificationChip(me.verificationLabel)
                            }
                            Text("UID: ${me.uid}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    if (!me.bio.isNullOrBlank()) {
                        Text(me.bio ?: "", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 12.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row {
                        Stat(me.followerCount, "粉丝"); Spacer(Modifier.width(20.dp))
                        Stat(me.followingCount, "关注")
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onUpload, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Upload, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("上传视频")
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    runCatching { ZawaRepo.logout() }
                                    AuthStore.clear()
                                    myVideos.clear()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Logout, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("退出")
                        }
                    }
                }
            }
            item {
                Text("我的视频 · ${myVideos.size}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
            }
            if (loading && myVideos.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (myVideos.isEmpty()) {
                item { Text("还没有上传过视频", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(14.dp)) }
            } else {
                val rows = myVideos.chunked(2)
                items(rows, key = { it.first().id }) { rowVideos ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowVideos.forEach { v ->
                            Box(Modifier.weight(1f)) { MyVideoCard(v) { onVideo(v.id) } }
                        }
                        if (rowVideos.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
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
private fun MyVideoCard(v: Video, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(5.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(96.dp)) {
            com.zawa.client.ui.components.VideoThumb(v.coverAbs, Modifier.fillMaxSize())
            Surface(
                color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp)
            ) {
                Text("${countFmt(v.views)} 播放", color = androidx.compose.ui.graphics.Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
            }
        }
        Text(v.title, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp))
    }
}
