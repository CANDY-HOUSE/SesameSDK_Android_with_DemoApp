package co.candyhouse.app.util

import android.content.res.AssetManager
import android.net.Uri
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import co.candyhouse.sesame.utils.L
import java.io.ByteArrayInputStream
import java.io.IOException

/** Serves the packaged SPA at its configured origin; cloud and external URLs remain online. */
internal class BundledWebResources(private val assets: AssetManager, origin: String) {
    private val originUri = Uri.parse(origin)

    fun intercept(request: WebResourceRequest): WebResourceResponse? {
        val uri = request.url
        if (uri.scheme != originUri.scheme || uri.host != originUri.host || uri.port != originUri.port) return null
        if (request.method != "GET") return error(405, "Method Not Allowed")

        val path = uri.path.orEmpty().removePrefix("/")
        if (path.split('/').any { it == ".." || it == "." } || '\\' in path) return error(404, "Not Found")
        // BrowserRouter routes (including reload/logout) use the same local document.
        val resource = if (request.isForMainFrame) "index.html" else path
        val stream = try {
            assets.open("biz/$resource")
        } catch (_: IOException) {
            L.e("WebResources", "APK resource missing; status=404; remoteFallback=false")
            // Never fall back to the remote deployment for missing bundle resources.
            return error(404, "Not Found")
        }
        if (resource == "index.html" || resource.startsWith("static/") && (resource.endsWith(".js") || resource.endsWith(".css"))) {
            L.i("WebResources", "source=APK; resource=assets/biz/$resource; status=200")
        }
        val mime = when (resource.substringAfterLast('.', "").lowercase()) {
            "js", "mjs" -> "application/javascript"
            "json", "map" -> "application/json"
            "svg" -> "image/svg+xml"
            "wasm" -> "application/wasm"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(resource.substringAfterLast('.').lowercase())
                ?: "application/octet-stream"
        }
        return WebResourceResponse(mime, "UTF-8", 200, "OK", headers, stream)
    }

    private fun error(status: Int, reason: String) = WebResourceResponse(
        "text/plain", "UTF-8", status, reason, headers, ByteArrayInputStream(reason.toByteArray())
    )

    private val headers = mapOf("Cache-Control" to "no-store", "X-Sesame-Resource" to "apk")
}
