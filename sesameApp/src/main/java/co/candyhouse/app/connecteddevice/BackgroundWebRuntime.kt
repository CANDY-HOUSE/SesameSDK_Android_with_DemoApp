package co.candyhouse.app.connecteddevice

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.SesameApp
import co.candyhouse.app.ble.BleRegistrationController
import co.candyhouse.app.bridge.NativeRequestHandler
import co.candyhouse.app.bridge.WebViewBridge
import co.candyhouse.app.data.KeyHandoff
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import kotlin.time.Duration.Companion.milliseconds

/** Reuses Biz's authenticated WebSocket runtime while no Activity owns the cloud bridge. */
@SuppressLint("SetJavaScriptEnabled")
class BackgroundWebRuntime(private val app: SesameApp) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val registration = BleRegistrationController(scope, {}, { byteArrayOf() }, {})
    private val ready = CompletableDeferred<Unit>()
    private val requests = NativeRequestHandler(
        app, app.ble, registration, KeyHandoff(app),
        language = { app.resources.configuration.locales[0].toLanguageTag() }, requestBle = {}, startDiscovery = {}, stopDiscovery = {},
        registeredKey = { null }, scanQr = {}, showRoute = {}, pushInfo = { JSONObject() }, notificationSettings = {}, openWebPage = {}, openExternal = {})
    private val bridge = WebViewBridge(BuildConfig.WEB_ORIGIN, scope) { json, reply ->
        requests.handle(json, reply)
        if (json.optString("action") == "home.devices") ready.complete(Unit)
    }
    private val observer: (JSONArray) -> Unit = { bridge.emit("snapshot", it) }
    private val web = WebView(app)

    init {
        app.ble.observers.add(observer)
        app.bleBackend.requestCloud = { op, body, id ->
            withTimeout(30000.milliseconds) { ready.await() }
            bridge.requestCloud(op, body, id)
        }
        web.settings.apply { javaScriptEnabled = true; domStorageEnabled = true; allowFileAccess = false; userAgentString += " SesameAndroid/1" }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = !bridge.isTrusted(request.url)
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                bridge.reset()
            }

            override fun onPageFinished(view: WebView, url: String?) {
                bridge.connect(view, url)
            }
        }
        web.loadUrl("${BuildConfig.WEB_ORIGIN}/?appHome=1&fromType=app&background=1")
    }

    fun close() {
        app.ble.observers.remove(observer); ready.cancel(); bridge.reset(); registration.close(); web.destroy(); scope.cancel()
    }
}
