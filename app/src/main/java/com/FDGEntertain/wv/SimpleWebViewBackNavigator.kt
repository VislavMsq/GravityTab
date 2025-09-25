package com.FDGEntertain.wv

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.core.view.size
import com.FDGEntertain.repositoryWebView.WebViewBackNavigator

class SimpleWebViewBackNavigator(
    private val activity: ComponentActivity,
    private val root: ViewGroup,
    private val progress: View,
    private val webViews: MutableList<WebView>
) : WebViewBackNavigator {
    override fun install() {
        // Привязка к жизненному циклу Activity — колбэк снимется сам при destroy
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
        // 3) Если это единственный WebView
        if (top.size == 1) top.canGoBack() else activity.finish()
        // 4) В стеке больше одного WebView
        if (top.canGoBack()){
            top.goBack()
        } else{
            root.removeView(top)
            top.destroy()
            webViews.removeAt(webViews.lastIndex)
        }
    }


}