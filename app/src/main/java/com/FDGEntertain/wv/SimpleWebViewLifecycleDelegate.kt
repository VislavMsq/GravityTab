package com.FDGEntertain.wv

import android.webkit.CookieManager
import android.webkit.WebView
import com.FDGEntertain.repository.WebViewLifecycleDelegate

class SimpleWebViewLifecycleDelegate(
    private val webViews: List<WebView>,
    private val cookieManager: CookieManager = CookieManager.getInstance()
) : WebViewLifecycleDelegate {
    override fun onResume() {
        cookieManager.flush()
        webViews.lastOrNull()?.onResume()
    }

    override fun onPause() {
        cookieManager.flush()
        webViews.lastOrNull()?.onPause()
    }

}