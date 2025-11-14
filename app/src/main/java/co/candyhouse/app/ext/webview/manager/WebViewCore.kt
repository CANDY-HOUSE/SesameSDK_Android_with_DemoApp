package co.candyhouse.app.ext.webview.manager

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi

/**
 * WebView通用配置和工具类
 *
 * @author frey on 2025/11/12
 */
object WebViewCore {

    /**
     * 通用WebView设置
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun applyCommonSettings(webView: WebView, supportZoom: Boolean = false) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(supportZoom)
            builtInZoomControls = supportZoom
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            allowFileAccess = false
            allowContentAccess = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
        }
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY
    }

    /**
     * 创建通用WebViewClient
     */
    fun createWebViewClient(
        onSchemeIntercept: ((Uri, Map<String, String>) -> Unit)? = null,
        onPageStarted: ((String?) -> Unit)? = null,
        onPageFinished: ((String?) -> Unit)? = null,
        onError: ((String) -> Unit)? = null,
        onLoadingChanged: ((Boolean) -> Unit)? = null
    ): WebViewClient {
        return object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                onLoadingChanged?.invoke(true)
                onPageStarted?.invoke(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                onLoadingChanged?.invoke(false)
                onPageFinished?.invoke(url)
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
                    onSchemeIntercept?.invoke(uri, params)
                    return true
                }

                // 处理非http/https协议
                if (scheme != "http" && scheme != "https") {
                    return try {
                        view?.context?.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        true
                    } catch (_: Exception) {
                        false
                    }
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
                    onError?.invoke(error?.description?.toString() ?: "Load error")
                }
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                onError?.invoke(description ?: "Load error")
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                err: SslError?
            ) {
                handler?.cancel()
                onError?.invoke("SSL error")
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                if (request?.isForMainFrame == true) {
                    onError?.invoke("HTTP ${errorResponse?.statusCode ?: ""}")
                }
            }
        }
    }

    /**
     * 清理WebView
     */
    fun cleanupWebView(webView: WebView?) {
        webView?.let { wv ->
            wv.stopLoading()
            wv.clearCache(true)
            wv.clearHistory()
            wv.clearFormData()
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            wv.removeAllViews()
            wv.destroy()
        }
    }
}