package co.candyhouse.app.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import co.candyhouse.app.util.openExternalUrl

/** A separate, unprivileged https page above the Biz WebView, which keeps its document and bridge alive underneath. */
class ExternalPage(
    private val activity: Activity, private val container: FrameLayout, private val chrome: () -> WebChromeClient,
    private val started: () -> Unit, private val finished: () -> Unit, private val gone: () -> Unit
) {
    var view: WebView? = null
        private set

    @SuppressLint("SetJavaScriptEnabled")
    fun open(url: String) {
        val page = WebView(activity)
        view = page
        page.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = true
        }
        page.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                if (request.url.scheme == "https") return false
                if (request.url.scheme in setOf("http", "mailto", "tel")) {
                    runCatching { activity.openExternalUrl(request.url) }
                }
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                if (view === this@ExternalPage.view) started()
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (view === this@ExternalPage.view && view.url == url) finished()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (view === this@ExternalPage.view && request.isForMainFrame) finished()
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                if (view === this@ExternalPage.view && request.isForMainFrame) finished()
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                if (view === this@ExternalPage.view) gone()
                return true
            }
        }
        page.webChromeClient = chrome()
        container.addView(page, FrameLayout.LayoutParams(-1, -1))
        page.loadUrl(url)
    }

    /** Goes back within the page; returns false when it has no history left. */
    fun back(): Boolean {
        val page = view ?: return false
        if (!page.canGoBack()) return false
        page.goBack(); return true
    }

    /** Destroys the page; returns false when none was open. */
    fun close(): Boolean {
        val page = view ?: return false
        view = null
        container.removeView(page)
        page.stopLoading()
        page.destroy()
        return true
    }
}
