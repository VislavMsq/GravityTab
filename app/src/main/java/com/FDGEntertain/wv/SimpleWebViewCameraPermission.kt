package com.FDGEntertain.wv

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.FDGEntertain.repositoryWebView.WebViewCameraPermission

class SimpleWebViewCameraPermission(
    caller: ActivityResultCaller,
    private val context: Context
) : WebViewCameraPermission {

    private var pendingRequest: PermissionRequest? = null

    private val permissionLauncher = caller.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingRequest?.let { request ->
            if (granted) {
                request.grant(request.resources)
            } else {
                request.deny()
            }
        }
        pendingRequest = null
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        val appHasCamera = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (appHasCamera) {
            request.grant(request.resources)
        } else {
            pendingRequest?.deny()
            pendingRequest = request
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun clearPending() {
        pendingRequest?.deny()
        pendingRequest = null
    }
}