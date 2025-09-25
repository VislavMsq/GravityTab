package com.FDGEntertain.repositoryWebView

import android.webkit.ConsoleMessage

interface WebViewConsoleHandler {
    fun onConsoleMessage(msg: ConsoleMessage?): Boolean

}