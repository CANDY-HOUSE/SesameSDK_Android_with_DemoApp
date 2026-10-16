package co.candyhouse.app.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient

/** Camera/microphone grants and file chooser for WebView pages; the page decides when to ask. */
class WebMediaAccess(private val activity: Activity, private val trusted: (Uri) -> Boolean, private val reload: () -> Unit) {
    private var files: ValueCallback<Array<Uri>>? = null
    private var pendingRequest: PermissionRequest? = null
    private var pendingResources = emptyArray<String>()
    private var retryAfterGrant = false

    fun request(request: PermissionRequest) {
        val mediaResources = request.resources.filter {
            it == PermissionRequest.RESOURCE_AUDIO_CAPTURE ||
                    it == PermissionRequest.RESOURCE_VIDEO_CAPTURE
        }.toTypedArray()
        if (!trusted(request.origin) || mediaResources.size != request.resources.size) {
            request.deny()
            return
        }

        val missingPermissions = buildList {
            if (
                mediaResources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE) &&
                activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.RECORD_AUDIO)
            if (
                mediaResources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) &&
                activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.CAMERA)
        }
        if (missingPermissions.isEmpty()) {
            request.grant(mediaResources)
        } else {
            cancel()
            pendingRequest = request
            pendingResources = mediaResources
            activity.requestPermissions(missingPermissions.toTypedArray(), RequestCode.WEB_MEDIA)
        }
    }

    fun cancelled(request: PermissionRequest) {
        if (pendingRequest === request) {
            pendingRequest = null
            pendingResources = emptyArray()
            retryAfterGrant = true
        }
    }

    fun permissionResult(results: IntArray) {
        val request = pendingRequest
        val resources = pendingResources
        pendingRequest = null
        pendingResources = emptyArray()
        val granted = results.isNotEmpty() && results.all { it == PackageManager.PERMISSION_GRANTED }
        val shouldRetry = retryAfterGrant
        retryAfterGrant = false
        if (granted) {
            if (request != null) request.grant(resources)
            else if (shouldRetry) reload()
        } else {
            request?.deny()
        }
    }

    fun chooseFile(callback: ValueCallback<Array<Uri>>, params: WebChromeClient.FileChooserParams): Boolean {
        files?.onReceiveValue(null); files = callback
        return runCatching { activity.startActivityForResult(params.createIntent(), RequestCode.FILE_CHOOSER); true }.getOrElse {
            files?.onReceiveValue(null); files = null; false
        }
    }

    fun fileResult(resultCode: Int, data: Intent?) {
        files?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data)); files = null
    }

    /** Denies an outstanding media request, e.g. when the page that asked is gone. */
    fun cancel() {
        val request = pendingRequest
        pendingRequest = null
        pendingResources = emptyArray()
        retryAfterGrant = false
        request?.deny()
    }

    fun close() {
        cancel()
        files?.onReceiveValue(null); files = null
    }
}
