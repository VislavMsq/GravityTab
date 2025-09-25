package com.FDGEntertain.repositoryWebView

import android.webkit.PermissionRequest

interface WebViewCameraPermission {
    fun onPermissionRequest(request: PermissionRequest)

    fun clearPending()
}