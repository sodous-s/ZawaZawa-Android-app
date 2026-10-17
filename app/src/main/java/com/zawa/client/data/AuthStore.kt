package com.zawa.client.data

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Persists the zawa.uk session cookie captured from the login WebView and exposes
 * it to the OkHttp-based API client. Also holds the signed-in user as Compose state
 * so the UI can react to login / logout. The site uses a cookie-based session
 * (`AUTH_REQUIRED` 401 for every authenticated endpoint), so the cookie *is* the
 * credential here.
 */
object AuthStore {
    private const val PREF = "zawa_auth"
    private const val KEY_COOKIE = "cookie"

    private var prefs: SharedPreferences? = null

    var user by mutableStateOf<ZawaUser?>(null)
        private set

    val isLoggedIn: Boolean get() = user != null

    fun init(ctx: Context) {
        if (prefs == null) {
            prefs = ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        }
    }

    /** Raw Cookie header value ("a=1; b=2"), or "" when signed out. */
    fun cookie(): String = prefs?.getString(KEY_COOKIE, "").orEmpty()

    fun saveCookie(raw: String?) {
        prefs?.edit()?.putString(KEY_COOKIE, raw?.trim().orEmpty())?.apply()
    }

    fun updateUser(u: ZawaUser?) { user = u }

    /** Replay the saved cookie into the WebView store so later WebViews stay signed in. */
    fun syncCookieToWebView() {
        val raw = cookie()
        if (raw.isBlank()) return
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        raw.split(";").forEach { part ->
            val p = part.trim()
            if (p.isNotEmpty() && p.contains("=")) cm.setCookie(BASE, p)
        }
        cm.flush()
    }

    /** Read the live cookie from the WebView store (used while the login WebView runs). */
    fun readWebViewCookie(): String = CookieManager.getInstance().getCookie(BASE).orEmpty()

    fun clearWebViewCookies() {
        val cm = CookieManager.getInstance()
        cm.removeAllCookies(null)
        cm.flush()
    }

    fun clear() {
        saveCookie("")
        user = null
        clearWebViewCookies()
    }
}
