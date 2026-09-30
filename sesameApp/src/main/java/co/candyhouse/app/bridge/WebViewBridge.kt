package co.candyhouse.app.bridge

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.WebMessage
import android.webkit.WebMessagePort
import android.webkit.WebView
import androidx.core.net.toUri
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

/** Owns the trusted message channel and invalidates requests when its document changes. */
class WebViewBridge(
    origin: String,
    private val scope: CoroutineScope,
    private val handle: suspend (JSONObject, (Boolean, Any?, String?) -> Unit) -> Unit
) {
    private val originUri = origin.toUri()
    private var port: WebMessagePort? = null
    private val pending = mutableMapOf<String, CompletableDeferred<Any?>>()
    private var generation = 0
    private var documentScope: CoroutineScope? = null

    fun isTrusted(uri: Uri): Boolean {
        val defaultPort = if (originUri.scheme == "https") 443 else 80
        return uri.scheme == originUri.scheme && uri.host == originUri.host &&
                (if (uri.port == -1) defaultPort else uri.port) == (if (originUri.port == -1) defaultPort else originUri.port)
    }

    fun connect(view: WebView, url: String?) {
        if (!isTrusted((url ?: "").toUri()) || port != null) return
        val epoch = generation
        val requests = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))
        documentScope = requests
        val channel = view.createWebMessageChannel()
        port = channel[0]
        channel[0].setWebMessageCallback(object : WebMessagePort.WebMessageCallback() {
            override fun onMessage(source: WebMessagePort, message: WebMessage) {
                if (epoch != generation || !isTrusted((view.url ?: "").toUri())) return
                val json = runCatching { JSONObject(message.data ?: "") }.getOrNull() ?: return
                if (json.optString("action") == "home.cloudResponse") {
                    pending.remove(json.optString("requestId"))?.let { result ->
                        if (json.optBoolean("success")) result.complete(json.opt("data").takeUnless { it == JSONObject.NULL })
                        else result.completeExceptionally(IllegalStateException("Cloud operation failed"))
                    }
                    return
                }
                requests.launch {
                    handle(json) { ok, data, error ->
                        requests.launch {
                            if (epoch == generation && isTrusted((view.url ?: "").toUri())) {
                                emit(
                                    "result", JSONObject().put("requestId", json.optString("requestId"))
                                        .put("success", ok).put("data", data ?: JSONObject.NULL).put("error", error)
                                )
                            }
                        }
                    }
                }
            }
        }, Handler(Looper.getMainLooper()))
        val nonce = UUID.randomUUID().toString()
        view.evaluateJavascript("window.__sesamePortNonce = ${JSONObject.quote(nonce)}") {
            if (epoch == generation && isTrusted((view.url ?: "").toUri())) {
                view.postWebMessage(WebMessage(nonce, arrayOf(channel[1])), originUri)
            } else {
                channel.forEach { it.close() }
            }
        }
    }

    fun emit(type: String, data: Any = JSONObject()) {
        port?.postMessage(WebMessage(JSONObject().put("type", type).put("data", data).toString()))
    }

    suspend fun requestCloud(operation: String, body: JSONObject, deviceId: String?): Any? = withContext(Dispatchers.Main.immediate) {
        check(port != null) { "Web page unavailable" }
        val id = UUID.randomUUID().toString()
        val response = CompletableDeferred<Any?>()
        pending[id] = response
        try {
            emit(
                "cloudRequest", JSONObject().put("requestId", id).put("op", operation)
                    .put("body", body).put("deviceUUID", deviceId)
            )
            withTimeout(30000.milliseconds) { response.await() }
        } finally {
            pending.remove(id)
        }
    }

    fun reset() {
        pending.values.forEach { it.completeExceptionally(IllegalStateException("Web page changed")) }
        pending.clear()
        generation++
        documentScope?.cancel()
        documentScope = null
        port?.close()
        port = null
    }
}
