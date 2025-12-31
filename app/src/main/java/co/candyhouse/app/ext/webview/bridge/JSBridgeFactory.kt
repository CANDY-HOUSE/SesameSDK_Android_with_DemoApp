package co.candyhouse.app.ext.webview.bridge

import android.content.Context
import android.webkit.WebView
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.CoroutineScope

/**
 * JS Bridge工厂，用于创建和管理JS Bridge
 *
 * @author frey on 2025/11/12
 */
object JSBridgeFactory {

    /**
     * 判断场景是否需要JS Bridge
     */
    fun needsJSBridge(scene: String): Boolean {
        return scene in setOf(
            "device-setting",
            "me-index",
            "device-notify",
            "device-user",
            "ir-remote",
            "ir-types",
            "wifi-module"
        )
    }

    /**
     * 为WebView设置JS Bridge
     */
    fun setupJSBridge(
        webView: WebView,
        scene: String,
        scope: CoroutineScope,
        context: Context,
        deviceModel: CHDeviceViewModel? = null,
        onHeightChanged: ((Float) -> Unit)? = null,
        onRequestLogin: (() -> Unit)? = null,
        onRequestNotificationSettings: (() -> Unit)? = null,
        onRequestDestroySelf: (() -> Unit)? = null,
        onRequestRefreshApp: (() -> Unit)? = null,
        onRequestWifiConfig: (() -> Unit)? = null,
        onEnablePullRefresh: ((Boolean) -> Unit)? = null
    ): WebViewJSBridge? {
        if (!needsJSBridge(scene)) {
            return null
        }

        val config = WebViewJSBridge.JSBridgeConfig(
            onHeightChanged = onHeightChanged,
            onRequestLogin = onRequestLogin,
            onRequestNotificationSettings = onRequestNotificationSettings,
            onRequestDestroySelf = onRequestDestroySelf,
            onRequestRefreshApp = onRequestRefreshApp,
            onRequestWifiConfig = onRequestWifiConfig,
            onEnablePullRefresh = onEnablePullRefresh
        )

        val jsBridge = WebViewJSBridge(webView, scope, config)
        if (scene == "wifi-module") {
            jsBridge.hub3Bridge = Hub3JSBridge(
                webView = webView,
                scope = scope,
                context = context,
                deviceModel = deviceModel,
                onRequestWifiConfig = onRequestWifiConfig
            )
        }
        webView.addJavascriptInterface(jsBridge, "AndroidHandler")
        L.d("JSBridgeFactory", "Added JS Bridge for scene: $scene")

        return jsBridge
    }
}