package com.FDGEntertain.wv

import android.app.Activity
import android.content.Intent
import android.webkit.WebResourceRequest
import com.FDGEntertain.repositoryWebView.WebViewUrlHandler

class SimpleWebViewUrlHandler(
    private val activity: Activity
) : WebViewUrlHandler {
    override fun shouldOverride(request: WebResourceRequest): Boolean {
       val s = request.url.toString()
        if (s.startsWith("http")) return false
        return try {
            val intent = Intent.parseUri(s, Intent.URI_INTENT_SCHEME)
            activity.startActivity(intent)
            true
        }catch (_: Exception){
            true
        }
    }
}