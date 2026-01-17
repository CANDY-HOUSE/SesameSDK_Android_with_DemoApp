package co.candyhouse.app.ext.webview.manager

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView

/**
 * WebView池管理器 - 为每个场景维护独立的WebView实例
 *
 * @author frey on 2025/11/12
 */
object WebViewPoolManager {

    private val webViewMap = mutableMapOf<String, WebView>()
    private val stateMap = mutableMapOf<String, WebViewState>()

    data class WebViewState(
        var currentUrl: String? = null,
        var isInitialized: Boolean = false,
        var pendingRefresh: Boolean = false
    )

    /**
     * 获取或创建指定场景的WebView
     */
    fun getOrCreateWebView(
        context: Context,
        webViewName: String,
        onSchemeIntercept: ((Uri, Map<String, String>) -> Unit)? = null,
        onPageFinished: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null,
        onLoadingChanged: ((Boolean) -> Unit)? = null
    ): WebView? {
        val state = stateMap.getOrPut(webViewName) { WebViewState() }

        var webView = webViewMap[webViewName]
        if (webView == null) {
            webView = WebViewSafeInitializer.createWebViewSafely(context)

            if (webView == null) {
                onError?.invoke("WebView initialization failed")
                return null
            }

            WebViewCore.applyCommonSettings(webView, supportZoom = false)
            webViewMap[webViewName] = webView
        }
        webView.webViewClient = WebViewCore.createWebViewClient(
            onSchemeIntercept = onSchemeIntercept,
            onPageFinished = { url ->
                onPageFinished?.invoke()
                url?.let {
                    state.currentUrl = it
                    state.isInitialized = true
                }
            },
            onError = onError,
            onLoadingChanged = onLoadingChanged
        )
        (webView.parent as? ViewGroup)?.removeView(webView)
        return webView
    }

    fun shouldReload(webViewName: String): Boolean {
        val state = stateMap[webViewName]
        return state == null || !state.isInitialized || state.currentUrl == null
    }

    fun loadUrl(webViewName: String, url: String) {
        webViewMap[webViewName]?.loadUrl(url)
        stateMap[webViewName]?.apply {
            currentUrl = url
            isInitialized = true
        }
    }

    fun reload(webViewName: String) {
        webViewMap[webViewName]?.reload()
    }

    fun setPendingRefresh(webViewName: String) {
        stateMap[webViewName]?.pendingRefresh = true
    }

    fun checkAndConsumePendingRefresh(webViewName: String): Boolean {
        val state = stateMap[webViewName] ?: return false
        val needs = state.pendingRefresh
        state.pendingRefresh = false
        return needs
    }

    fun clearWebView(webViewName: String) {
        val webView = webViewMap.remove(webViewName)
        WebViewCore.cleanupWebView(webView)
        stateMap.remove(webViewName)
    }

    fun clearAll() {
        webViewMap.keys.toList().forEach { webViewName ->
            clearWebView(webViewName)
        }
    }
}