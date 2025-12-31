package co.candyhouse.app.ext.webview.bridge

import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.app.NotificationManagerCompat
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.utils.L
import co.utils.SharedPreferencesUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 统一的WebView JS Bridge
 * 支持原生Fragment和Compose两种使用方式
 *
 * @author frey on 2025/10/9
 */
class WebViewJSBridge(
    private val webView: WebView?,
    private val scope: CoroutineScope,
    private val config: JSBridgeConfig = JSBridgeConfig()
) {

    private val tag = "WebViewJSBridge"
    var hub3Bridge: Hub3JSBridge? = null

    data class JSBridgeConfig(
        val onHeightChanged: ((Float) -> Unit)? = null,
        val onRequestLogin: (() -> Unit)? = null,
        val onRequestNotificationSettings: (() -> Unit)? = null,
        val onRequestDestroySelf: (() -> Unit)? = null,
        val onRequestRefreshApp: (() -> Unit)? = null,
        val onRequestWifiConfig: (() -> Unit)? = null,
        val onEnablePullRefresh: ((Boolean) -> Unit)? = null
    )

    companion object {
        const val requestRefreshApp = "requestRefreshApp"
        const val requestEnablePullRefresh = "requestEnablePullRefresh"
        const val requestDestroySelf = "requestDestroySelf"
        const val requestAutoLayoutHeight = "requestAutoLayoutHeight"
        const val requestLogin = "requestLogin"
        const val requestPushToken = "requestPushToken"
        const val requestNotificationStatus = "requestNotificationStatus"
        const val requestNotificationSettings = "requestNotificationSettings"
        const val requestBLEConnect = "requestBLEConnect"
        const val requestConfigureInternet = "requestConfigureInternet"
        const val requestMonitorInternet = "requestMonitorInternet"
        const val requestDeviceFWUpgrade = "requestDeviceFWUpgrade"
    }

    @JavascriptInterface
    fun postMessage(message: String) {
        L.d(tag, "postMessage called with: $message")

        try {
            val json = JSONObject(message)
            val action = json.optString("action")
            val callbackName = json.optString("callbackName")

            L.d(tag, "Action: $action, Callback: $callbackName")

            when (action) {
                requestBLEConnect -> {
                    hub3Bridge?.handleRequestBLEConnect(json)
                }

                requestConfigureInternet -> {
                    hub3Bridge?.handleRequestConfigureInternet(json)
                }

                requestMonitorInternet -> {
                    hub3Bridge?.handleRequestMonitorInternet(json)
                }

                requestDeviceFWUpgrade -> {
                    hub3Bridge?.handleRequestDeviceFWUpgrade(json)
                }

                requestRefreshApp -> {
                    scope.launch(Dispatchers.Main) {
                        config.onRequestRefreshApp?.invoke()
                    }
                }

                requestDestroySelf -> {
                    scope.launch(Dispatchers.Main) {
                        config.onRequestDestroySelf?.invoke()
                    }
                }

                requestEnablePullRefresh -> {
                    scope.launch(Dispatchers.Main) {
                        config.onEnablePullRefresh?.invoke(true)
                    }
                }

                requestAutoLayoutHeight -> {
                    val height = json.optDouble("height", 0.0)
                    if (height > 0) {
                        scope.launch(Dispatchers.Main) {
                            config.onHeightChanged?.invoke(height.toFloat())
                        }
                    }
                }

                requestLogin -> {
                    scope.launch(Dispatchers.Main) {
                        config.onRequestLogin?.invoke()
                    }
                }

                requestPushToken -> {
                    SharedPreferencesUtils.deviceToken?.let { handleRequestPushToken(callbackName, it) }
                }

                requestNotificationStatus -> {
                    handleNotificationPermissionStatus(callbackName)
                }

                requestNotificationSettings -> {
                    scope.launch(Dispatchers.Main) {
                        config.onRequestNotificationSettings?.invoke()
                    }
                }

                else -> {
                    L.e(tag, "Unknown action: $action")
                }
            }
        } catch (e: Exception) {
            L.e(tag, "Error parsing message: ${e.message}")
        }
    }

    private fun handleRequestPushToken(callbackName: String, pushToken: String) {
        scope.launch {
            val responseData = JSONObject().apply {
                put("pushToken", pushToken)
            }

            sendResponseDataToH5(callbackName, responseData)
        }
    }

    private fun handleNotificationPermissionStatus(callbackName: String) {
        scope.launch {
            val isEnabled = NotificationManagerCompat.from(CHDeviceManager.app).areNotificationsEnabled()

            val responseData = JSONObject().apply {
                put("enabled", isEnabled)
            }

            sendResponseDataToH5(callbackName, responseData)
        }
    }

    private fun sendResponseDataToH5(callbackName: String, responseData: Any) {
        scope.launch(context = Dispatchers.Main) {
            val jsCode = "if(window.$callbackName) window.$callbackName($responseData);"
            webView?.evaluateJavascript(jsCode, null)
        }
    }

    fun cleanup() {
        hub3Bridge?.cleanup()
        hub3Bridge = null
    }
}