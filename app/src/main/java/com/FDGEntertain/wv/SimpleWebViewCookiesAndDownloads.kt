// com/FDGEntertain/wv/SimpleWebViewCookiesAndDownloads.kt
package com.FDGEntertain.wv

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebView
import com.FDGEntertain.repositoryWebView.WebViewCookiesAndDownloads

class SimpleWebViewCookiesAndDownloads(
    private val activity: Activity,
    private val cookieManager: CookieManager = CookieManager.getInstance()
) : WebViewCookiesAndDownloads {

    override fun applyTo(view: WebView) {
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(view, true)

        view.setDownloadListener { url, _, _, _, _ ->
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}
