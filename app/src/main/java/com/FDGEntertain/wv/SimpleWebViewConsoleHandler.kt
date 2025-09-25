// com/FDGEntertain/wv/SimpleWebViewConsoleHandler.kt
package com.FDGEntertain.wv

import android.app.Activity
import android.content.Intent
import android.webkit.ConsoleMessage
import com.FDGEntertain.GameActivity
import com.FDGEntertain.repositoryWebView.WebViewConsoleHandler

class SimpleWebViewConsoleHandler(
    private val activity: Activity
) : WebViewConsoleHandler {

    // 1) Точка входа из WebChromeClient: берём строку и делегируем исполнение
    override fun onConsoleMessage(msg: ConsoleMessage?): Boolean {
        val command = msg?.message()
        return executeCommand(command)
    }

    // 2) Исполнение команды; всегда возвращаем true (как и раньше)
    private fun executeCommand(command: String?): Boolean {
        if (command == "openFlipBoard") {
            activity.startActivity(Intent(activity, GameActivity::class.java))
            activity.finish()
        }
        return true
    }
}
