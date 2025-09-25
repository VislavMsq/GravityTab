package com.FDGEntertain.repositoryWebView

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient

interface WebViewFileChooser {
    fun onShowFileChooser(
        params: WebChromeClient.FileChooserParams?,
        callback: ValueCallback<Array<Uri>>?
    ): Boolean

    fun clearPending()
}