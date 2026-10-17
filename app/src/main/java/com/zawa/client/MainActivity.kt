package com.zawa.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zawa.client.data.AuthStore
import com.zawa.client.data.ZawaRepo
import com.zawa.client.ui.Routes
import com.zawa.client.ui.screens.AccountScreen
import com.zawa.client.ui.screens.ArticleListScreen
import com.zawa.client.ui.screens.ArticleReaderScreen
import com.zawa.client.ui.screens.LoginScreen
import com.zawa.client.ui.screens.ProfileScreen
import com.zawa.client.ui.screens.SearchScreen
import com.zawa.client.ui.screens.TagScreen
import com.zawa.client.ui.screens.UploadScreen
import com.zawa.client.ui.screens.VideoDetailScreen
import com.zawa.client.ui.screens.VideoFeedScreen
import com.zawa.client.ui.theme.ZawaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ZawaTheme { AppRoot() } }
    }
}

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun AppRoot() {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val tabs = listOf(
        BottomTab(Routes.VIDEOS, "视频", Icons.Default.VideoLibrary),
        BottomTab(Routes.ARTICLES, "专栏", Icons.Default.Article),
        BottomTab(Routes.SEARCH, "搜索", Icons.Default.Search),
        BottomTab(Routes.ACCOUNT, "我的", Icons.Default.Person)
    )
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = currentRoute in tabs.map { it.route }

    // Restore the persisted session cookie on cold start.
    LaunchedEffect(Unit) {
        AuthStore.init(ctx)
        if (AuthStore.cookie().isNotBlank()) {
            val me = runCatching { ZawaRepo.me() }.getOrNull()
            if (me != null) AuthStore.updateUser(me) else AuthStore.saveCookie("")
        }
    }

    fun goLogin() {
        nav.navigate(Routes.LOGIN) { launchSingleTop = true }
    }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Routes.VIDEOS,
            modifier = Modifier.fillMaxSize().padding(inner)
        ) {
            composable(Routes.VIDEOS) {
                VideoFeedScreen(
                    onVideo = { nav.navigate(Routes.video(it)) },
                    onUser = { nav.navigate(Routes.user(it)) }
                )
            }
            composable(Routes.ARTICLES) {
                ArticleListScreen(
                    onArticle = { nav.navigate(Routes.article(it)) },
                    onUser = { nav.navigate(Routes.user(it)) }
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(onTag = { nav.navigate(Routes.tag(it)) })
            }
            composable(Routes.ACCOUNT) {
                AccountScreen(
                    onLogin = { goLogin() },
                    onVideo = { nav.navigate(Routes.video(it)) },
                    onUpload = { nav.navigate(Routes.UPLOAD) }
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onBack = { nav.popBackStack() },
                    onLoggedIn = { nav.popBackStack() }
                )
            }
            composable(Routes.UPLOAD) {
                UploadScreen(onBack = { nav.popBackStack() })
            }
            composable(
                route = Routes.VIDEO,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("id") ?: return@composable
                VideoDetailScreen(
                    videoId = id,
                    onBack = { nav.popBackStack() },
                    onUser = { nav.navigate(Routes.user(it)) },
                    onLogin = { goLogin() }
                )
            }
            composable(
                route = Routes.ARTICLE,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("id") ?: return@composable
                ArticleReaderScreen(
                    articleId = id,
                    onBack = { nav.popBackStack() },
                    onUser = { nav.navigate(Routes.user(it)) }
                )
            }
            composable(
                route = Routes.USER,
                arguments = listOf(navArgument("uid") { type = NavType.StringType })
            ) { entry ->
                val uid = entry.arguments?.getString("uid")?.toLongOrNull() ?: return@composable
                ProfileScreen(
                    uid = uid,
                    onBack = { nav.popBackStack() },
                    onVideo = { nav.navigate(Routes.video(it)) },
                    onArticle = { nav.navigate(Routes.article(it)) },
                    onLogin = { goLogin() }
                )
            }
            composable(
                route = Routes.TAG,
                arguments = listOf(navArgument("tag") { type = NavType.StringType })
            ) { entry ->
                val tag = entry.arguments?.getString("tag") ?: return@composable
                TagScreen(
                    tag = java.net.URLDecoder.decode(tag, "UTF-8"),
                    onVideo = { nav.navigate(Routes.video(it)) },
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}
