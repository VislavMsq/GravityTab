package com.FDGEntertain.repositoryWebView

import android.webkit.WebResourceRequest

interface WebViewUrlHandler {
    fun shouldOverride(request: WebResourceRequest): Boolean
}