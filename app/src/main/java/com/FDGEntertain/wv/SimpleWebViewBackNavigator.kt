package com.FDGEntertain.wv

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.core.view.isVisible
import com.FDGEntertain.repositoryWebView.WebViewBackNavigator

class SimpleWebViewBackNavigator(
    private val activity: ComponentActivity,
    private val root: ViewGroup,
    private val progress: View,
    private val webViews: MutableList<WebView>
) : WebViewBackNavigator {

    override fun install() {
        activity.onBackPressedDispatcher.addCallback(activity) {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        progress.isVisible = false

        val top = webViews.lastOrNull() ?: run {
            activity.finish()
            return
        }

        if (webViews.size == 1) {
            if (top.canGoBack()) {
                top.goBack()
            } else {
                activity.finish()
            }
        } else {
            if (top.canGoBack()) {
                top.goBack()
            } else {
                root.removeView(top)
                top.destroy()
                webViews.removeAt(webViews.lastIndex)
            }
        }
    }
}
