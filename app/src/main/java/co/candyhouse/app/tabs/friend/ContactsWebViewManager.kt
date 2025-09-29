package co.candyhouse.app.tabs.friend

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi

/**
 * 通讯录WebView全局管理器
 *
 * @author frey on 2025/9/29
 */
@SuppressLint("StaticFieldLeak")
object ContactsWebViewManager {
    private var webView: WebView? = null
    private var currentSchemeInterceptor: ((Uri, Map<String, String>) -> Unit)? = null
    private var isInitialized = false
    private var currentUrl: String? = null
    private var pendingRefresh = false
    private var pendingDetailNavigation: Bundle? = null

    private var onPageStartedCallback: (() -> Unit)? = null
    private var onPageFinishedCallback: (() -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var onLoadingChangedCallback: ((Boolean) -> Unit)? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(
        context: Context,
        onSchemeIntercept: ((Uri, Map<String, String>) -> Unit)? = null,
        onPageStarted: (() -> Unit)? = null,
        onPageFinished: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null,
        onLoadingChanged: ((Boolean) -> Unit)? = null
    ): WebView {
        currentSchemeInterceptor = onSchemeIntercept
        onPageStartedCallback = onPageStarted
        onPageFinishedCallback = onPageFinished
        onErrorCallback = onError
        onLoadingChangedCallback = onLoadingChanged

        if (webView == null) {
            webView = WebView(context.applicationContext).apply {
                // WebView设置
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    allowFileAccess = false
                    allowContentAccess = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        safeBrowsingEnabled = true
                    }
                }

                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY

                // WebViewClient
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onLoadingChangedCallback?.invoke(true)
                        onPageStartedCallback?.invoke()
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onLoadingChangedCallback?.invoke(false)
                        onPageFinishedCallback?.invoke()
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val uri = request?.url ?: return false
                        val scheme = uri.scheme?.lowercase()

                        // 处理自定义scheme
                        if (scheme == "ssm") {
                            val params = uri.queryParameterNames.associateWith { key ->
                                uri.getQueryParameter(key) ?: ""
                            }
                            currentSchemeInterceptor?.invoke(uri, params)
                            return true
                        }

                        // 处理非http/https协议
                        if (scheme != "http" && scheme != "https") {
                            return false
                        }

                        return false
                    }

                    @RequiresApi(Build.VERSION_CODES.M)
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            onErrorCallback?.invoke(error?.description?.toString() ?: "Load error")
                        }
                    }
                }
            }
        }

        (webView?.parent as? ViewGroup)?.removeView(webView)

        return webView!!
    }

    fun isLoaded(): Boolean = isInitialized && currentUrl != null

    fun getCurrentUrl(): String? = currentUrl

    fun setCurrentUrl(url: String) {
        currentUrl = url
        isInitialized = true
    }

    fun shouldReload(forceRefresh: Boolean = false): Boolean {
        return forceRefresh || !isInitialized || currentUrl == null
    }

    fun reload() {
        webView?.reload()
    }

    fun loadUrl(url: String) {
        webView?.loadUrl(url)
        setCurrentUrl(url)
    }

    fun setPendingRefresh() {
        pendingRefresh = true
    }

    fun checkAndConsumePendingRefresh(): Boolean {
        val needs = pendingRefresh
        pendingRefresh = false
        return needs
    }

    fun setPendingDetailNavigation(email: String, subId: String) {
        pendingDetailNavigation = Bundle().apply {
            putString("scene", "contact-info")
            putString("email", email)
            putString("subUUID", subId.lowercase())
        }
    }

    fun checkAndConsumePendingDetail(): Bundle? {
        val bundle = pendingDetailNavigation
        pendingDetailNavigation = null
        return bundle
    }

    fun clear() {
        webView?.apply {
            stopLoading()
            clearCache(true)
            clearHistory()
            clearFormData()
            destroy()
        }
        webView = null
        isInitialized = false
        currentUrl = null
    }
}