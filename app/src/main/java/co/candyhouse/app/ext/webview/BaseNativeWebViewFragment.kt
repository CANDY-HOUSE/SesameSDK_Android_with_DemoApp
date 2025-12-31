package co.candyhouse.app.ext.webview

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewbinding.ViewBinding
import co.candyhouse.app.ext.webview.bridge.JSBridgeFactory.setupJSBridge
import co.candyhouse.app.ext.webview.bridge.WebViewJSBridge
import co.candyhouse.app.ext.webview.manager.WebViewPoolManager
import co.candyhouse.app.ext.webview.manager.WebViewUrlLoader
import co.candyhouse.app.tabs.HomeFragment
import co.candyhouse.sesame.utils.L
import co.utils.alertview.fragments.toastMSG
import kotlinx.coroutines.GlobalScope
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * 原生WebView Fragment基类 - 用于首页TAB
 *
 * @author frey on 2025/11/12
 */
abstract class BaseNativeWebViewFragment<T : ViewBinding> : HomeFragment<T>() {

    private val tag = "BaseNativeWebViewFragment"
    protected abstract val webViewName: String
    protected abstract fun getWebViewContainer(): ViewGroup
    protected abstract fun getLoadingView(): View
    protected abstract fun getSwipeRefreshLayout(): SwipeRefreshLayout?

    private var webView: WebView? = null
    private var jsBridge: WebViewJSBridge? = null
    private var isRefreshing = false
    private var isManualRefresh = false

    companion object {
        private val activeFragments = ConcurrentHashMap<String, WeakReference<BaseNativeWebViewFragment<*>>>()
    }

    override fun setupUI() {
        setupWebView()
        setupCustomUI()
    }

    override fun setupListeners() {
        getSwipeRefreshLayout()?.setOnRefreshListener {
            if (!isRefreshing) {
                performRefresh()
            }
        }
        setupCustomListeners()
    }

    protected open fun setupCustomUI() {}
    protected open fun setupCustomListeners() {}

    private fun setupWebView() {
        webView = WebViewPoolManager.getOrCreateWebView(
            requireContext(),
            webViewName = webViewName,
            onSchemeIntercept = { uri, params ->
                if (isAdded) {
                    handleSchemeIntercept(uri, params)
                }
            },
            onPageFinished = {
                if (isAdded) {
                    finishManualRefresh()
                }
            },
            onError = { _ ->
                if (isAdded) {
                    getLoadingView().visibility = View.GONE
                    finishManualRefresh()
                }
            },
            onLoadingChanged = { isLoading ->
                if (isAdded && !isManualRefresh) {
                    getLoadingView().visibility = if (isLoading) View.VISIBLE else View.GONE
                }
            }
        )

        val container = getWebViewContainer()
        container.removeAllViews()
        container.addView(webView)

        setupJSBridge()

        if (WebViewPoolManager.shouldReload(webViewName)) {
            WebViewPoolManager.checkAndConsumePendingRefresh(webViewName)
            getLoadingView().visibility = View.VISIBLE
            loadWebContent()
        }
    }

    private fun loadWebContent() {
        if (!isManualRefresh) {
            getLoadingView().visibility = View.VISIBLE
        }

        WebViewUrlLoader.loadWebUrl(
            scene = webViewName,
            extInfo = getExtInfo(),
            onSuccess = { url ->
                activity?.runOnUiThread {
                    if (isAdded) {
                        WebViewPoolManager.loadUrl(webViewName, url)
                    }
                }
            },
            onError = { error ->
                activity?.runOnUiThread {
                    if (isAdded) {
                        getLoadingView().visibility = View.GONE
                        finishManualRefresh()
                        if (error.contains("Please log in", ignoreCase = true)) {
                            toastMSG("Please log in")
                        } else {
                            toastMSG(error)
                        }
                    }
                }
            }
        )
    }

    private fun performRefresh() {
        if (isRefreshing) return
        isRefreshing = true
        isManualRefresh = true
        loadWebContent()
    }

    private fun finishManualRefresh() {
        if (isManualRefresh) {
            getSwipeRefreshLayout()?.isRefreshing = false
            isRefreshing = false
            isManualRefresh = false
        }
    }

    override fun onResume() {
        super.onResume()
        activeFragments[webViewName] = WeakReference(this)

        if (WebViewPoolManager.checkAndConsumePendingRefresh(webViewName)) {
            reloadRefresh()
        }
    }

    override fun onPause() {
        super.onPause()
        activeFragments[webViewName]?.get()?.let { fragment ->
            if (fragment == this) {
                activeFragments.remove(webViewName)
            }
        }

        getSwipeRefreshLayout()?.isRefreshing = false
        getLoadingView().visibility = View.GONE
        isRefreshing = false
        isManualRefresh = false
    }

    override fun onDestroyView() {
        jsBridge = null
        getWebViewContainer().removeAllViews()
        webView = null
        super.onDestroyView()
    }

    private fun setupJSBridge() {
        webView?.let { wv ->
            wv.removeJavascriptInterface("AndroidHandler")

            jsBridge = setupJSBridge(
                webView = wv,
                scene = webViewName,
                scope = GlobalScope,
                context = requireContext(),
                onRequestLogin = {
                    activeFragments[webViewName]?.get()?.let { fragment ->
                        if (fragment.isAdded && fragment.isVisible) {
                            try {
                                fragment.getOnRequestLogin()?.invoke()
                            } catch (e: Exception) {
                                L.e(tag, "Error invoking login callback", e)
                            }
                        } else {
                            L.d(tag, "Fragment not ready: isAdded=${fragment.isAdded}, isVisible=${fragment.isVisible}")
                        }
                    } ?: run {
                        L.d(tag, "No active fragment for $webViewName")
                    }
                }
            )
        }
    }

    fun reloadRefresh() {
        getSwipeRefreshLayout()?.isRefreshing = true
        performRefresh()
    }

    protected abstract fun handleSchemeIntercept(uri: Uri, params: Map<String, String>)
    protected open fun getExtInfo(): Map<String, String>? = null
    protected open fun getOnRequestLogin(): (() -> Unit)? = null
}