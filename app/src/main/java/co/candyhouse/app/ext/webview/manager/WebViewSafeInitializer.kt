package co.candyhouse.app.ext.webview.manager

import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import co.candyhouse.sesame.utils.L

/**
 * WebView 安全初始化工具类
 *
 * @author frey on 2026/1/17
 */
object WebViewSafeInitializer {
    private const val TAG = "WebViewSafeInit"

    @Volatile
    private var isProviderInitialized = false

    @Volatile
    private var initializationFailed = false

    /**
     * 在 Application 中调用，预初始化 WebView Provider
     */
    fun preloadWebViewProvider(context: Context) {
        if (isProviderInitialized || initializationFailed) return

        try {
            // 使用 getDefaultUserAgent 触发 Provider 初始化
            // 这比创建 WebView 实例更轻量
            WebSettings.getDefaultUserAgent(context)
            isProviderInitialized = true
            L.d(TAG, "WebView provider preloaded successfully")
        } catch (e: Exception) {
            L.e(TAG, "WebView provider preload failed", e)
            initializationFailed = true
        }
    }

    /**
     * 安全创建 WebView，带异常处理
     */
    fun createWebViewSafely(context: Context): WebView? {
        return try {
            if (initializationFailed) {
                L.w(TAG, "WebView provider init previously failed, skip creating")
                return null
            }

            WebView(context.applicationContext).also {
                isProviderInitialized = true
            }
        } catch (e: Exception) {
            L.e(TAG, "Failed to create WebView", e)
            initializationFailed = true
            null
        }
    }

    /**
     * 检查 WebView 是否可用
     */
    fun isWebViewAvailable(): Boolean {
        return !initializationFailed
    }
}