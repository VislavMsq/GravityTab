package com.FDGEntertain.wv

import android.content.ActivityNotFoundException
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import com.FDGEntertain.repositoryWebView.WebViewFileChooser

class SimpleWebViewFileChooser(
    caller: ActivityResultCaller
) : WebViewFileChooser {

    private var pending: ValueCallback<Array<Uri>>? = null
    private val launcher =
        caller.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            pending?.onReceiveValue(uris)
            pending = null
        }

    override fun onShowFileChooser(
        params: WebChromeClient.FileChooserParams?,
        callback: ValueCallback<Array<Uri>>?
    ): Boolean {
        pending?.onReceiveValue(null)
        pending = callback

        return try {
            launcher.launch(params?.createIntent()!!)
            true
        } catch (_: ActivityNotFoundException) {
            pending?.onReceiveValue(null)
            pending = null
            false
        }
    }

    override fun clearPending() {
        pending?.onReceiveValue(null)
        pending = null
    }
}