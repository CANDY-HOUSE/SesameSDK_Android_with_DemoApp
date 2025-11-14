package co.candyhouse.app.ext.webview.bridge

import android.webkit.WebView
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
            "device-user"
        )
    }

    /**
     * 为WebView设置JS Bridge
     */
    fun setupJSBridge(
        webView: WebView,
        scene: String,
        scope: CoroutineScope,
        onHeightChanged: ((Float) -> Unit)? = null,
        onRequestLogin: (() -> Unit)? = null,
        onRequestNotificationSettings: (() -> Unit)? = null
    ): WebViewJSBridge? {
        if (!needsJSBridge(scene)) {
            return null
        }

        val config = WebViewJSBridge.JSBridgeConfig(
            onHeightChanged = onHeightChanged,
            onRequestLogin = onRequestLogin,
            onRequestNotificationSettings = onRequestNotificationSettings
        )

        val jsBridge = WebViewJSBridge(webView, scope, config)
        webView.addJavascriptInterface(jsBridge, "AndroidHandler")
        L.d("JSBridgeFactory", "Added JS Bridge for scene: $scene")

        return jsBridge
    }
}