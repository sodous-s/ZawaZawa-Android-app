package com.zawa.client.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.zawa.client.data.AuthStore
import com.zawa.client.data.BASE

/**
 * Video upload runs through the real site's /upload wizard in a WebView. The wizard
 * is a multi-step flow (create draft -> PUT metadata -> proxy-put the source/cover to
 * S3) guarded by the same CAPTCHA as login, so reusing the signed-in WebView session
 * is far more robust than re-implementing it natively.
 */
@Composable
fun UploadScreen(onBack: () -> Unit) {
    val webRef = remember { mutableStateOf<WebView?>(null) }
    val pending = remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val chooser = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val cb = pending.value
        pending.value = null
        cb?.onReceiveValue(fileChooserUris(result.resultCode, result.data))
    }

    BackHandler {
        val wv = webRef.value
        if (wv != null && wv.canGoBack()) wv.goBack() else onBack()
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("上传视频", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                AuthStore.syncCookieToWebView()
                WebView(ctx).apply {
                    webRef.value = this
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.allowContentAccess = true
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = WebViewClient()
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowFileChooser(
                            view: WebView?,
                            filePathCallback: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            pending.value?.onReceiveValue(null)
                            pending.value = filePathCallback
                            val intent = fileChooserParams?.createIntent() ?: return false
                            return try {
                                chooser.launch(intent)
                                true
                            } catch (e: Exception) {
                                pending.value = null
                                false
                            }
                        }
                    }
                    loadUrl("$BASE/upload")
                }
            }
        )
    }
}

private fun fileChooserUris(resultCode: Int, data: Intent?): Array<Uri> {
    if (resultCode != Activity.RESULT_OK || data == null) return emptyArray()
    data.clipData?.let { clip ->
        return Array(clip.itemCount) { clip.getItemAt(it).uri }
    }
    val single = data.data
    return if (single != null) arrayOf(single) else emptyArray()
}
