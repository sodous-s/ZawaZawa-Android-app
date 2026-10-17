package com.zawa.client.ui.screens

import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.zawa.client.data.AuthStore
import com.zawa.client.data.BASE
import com.zawa.client.data.ZawaRepo
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Sign-in via a real WebView pointed at zawa.uk/login. The site protects login with
 * Geetest / Turnstile CAPTCHAs, so a native form is not viable — the WebView handles
 * the CAPTCHA and cookie handling, then we lift the resulting session cookie into
 * [AuthStore] for the native (OkHttp) API.
 */
@Composable
fun LoginScreen(onBack: () -> Unit, onLoggedIn: () -> Unit) {
    var checking by remember { mutableStateOf(false) }
    var forceProbe by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var lastCookie = ""
        while (isActive) {
            delay(1200)
            val cookie = AuthStore.readWebViewCookie()
            if (cookie.isBlank()) continue
            if (cookie == lastCookie && !forceProbe) continue
            lastCookie = cookie
            forceProbe = false
            checking = true
            val me = runCatching { ZawaRepo.me(cookie) }.getOrNull()
            checking = false
            if (me != null) {
                AuthStore.saveCookie(cookie)
                AuthStore.updateUser(me)
                onLoggedIn()
                return@LaunchedEffect
            }
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                "登录 zawa.uk",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (checking) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
            }
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                // Replay any previously captured cookie before loading so an existing
                // session is reused instead of asking the user to log in again.
                AuthStore.syncCookieToWebView()
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Left the login form (usually redirected to home) -> probe auth.
                            if (url != null && !url.contains("/login")) forceProbe = true
                        }
                    }
                    loadUrl("$BASE/login")
                }
            }
        )
    }
}
