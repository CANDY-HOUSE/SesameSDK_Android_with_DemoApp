package co.candyhouse.app.ext.webview

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.lifecycle.Lifecycle
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
    private var isRendererRecoveryPending = false
    private var connectivityManager: ConnectivityManager? = null
    private var networkRecoveryCallback: ConnectivityManager.NetworkCallback? = null

    companion object {
        private val activeFragments = ConcurrentHashMap<String, WeakReference<BaseNativeWebViewFragment<*>>>()
    }

    override fun setupUI() {
        if (isRendererRecoveryPending) {
            recoverWebViewWhenNetworkAvailable()
        } else {
            setupWebView()
        }
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
            },
            onRenderProcessGone = { didCrash ->
                L.w(tag, "WebView renderer exited: scene=$webViewName, didCrash=$didCrash")
                webView = null
                jsBridge = null
                isRendererRecoveryPending = true

                if (isAdded && view != null &&
                    lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                ) {
                    recoverWebViewWhenNetworkAvailable()
                }
            }
        )

        if (webView == null) {
            getLoadingView().visibility = View.GONE
            return
        }

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

    /**
     * Renderer 退出后的恢复入口。网络未通过系统验证时只监听一次，
     * 网络恢复后先注销监听，再重建 WebView 并重新请求页面 URL。
     */
    private fun recoverWebViewWhenNetworkAvailable() {
        if (!isAdded || view == null || webView != null) return

        val manager = connectivityManager ?: run {
            val appContext = requireContext().applicationContext
            (appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                ?.also { connectivityManager = it }
        } ?: return

        if (hasValidatedNetwork(manager)) {
            unregisterNetworkRecoveryCallback()
            isRendererRecoveryPending = false
            setupWebView()
            return
        }

        if (networkRecoveryCallback != null) return

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                    !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                ) {
                    return
                }

                activity?.runOnUiThread {
                    if (networkRecoveryCallback !== this) return@runOnUiThread
                    unregisterNetworkRecoveryCallback()

                    if (isAdded && view != null && webView == null &&
                        lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                    ) {
                        isRendererRecoveryPending = false
                        setupWebView()
                    }
                }
            }
        }

        networkRecoveryCallback = callback
        runCatching { manager.registerDefaultNetworkCallback(callback) }
            .onFailure {
                networkRecoveryCallback = null
                L.e(tag, "Failed to register network recovery callback", it)
            }
    }

    private fun hasValidatedNetwork(manager: ConnectivityManager): Boolean {
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun unregisterNetworkRecoveryCallback() {
        val callback = networkRecoveryCallback ?: return
        networkRecoveryCallback = null
        runCatching { connectivityManager?.unregisterNetworkCallback(callback) }
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

        if (webView == null && view != null) {
            recoverWebViewWhenNetworkAvailable()
        }

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
        unregisterNetworkRecoveryCallback()
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
                onRequestLogin = { loginUrl ->
                    activeFragments[webViewName]?.get()?.let { fragment ->
                        if (fragment.isAdded && fragment.isVisible) {
                            try {
                                fragment.getOnRequestLogin()?.invoke(loginUrl)
                            } catch (e: Exception) {
                                L.e(tag, "Error invoking login callback", e)
                            }
                        } else {
                            L.d(tag, "Fragment not ready: isAdded=${fragment.isAdded}, isVisible=${fragment.isVisible}")
                        }
                    } ?: run {
                        L.d(tag, "No active fragment for $webViewName")
                    }
                },
                onSignOutSucceeded = {
                    activeFragments[webViewName]?.get()?.let { fragment ->
                        if (fragment.isAdded) {
                            try {
                                fragment.getOnSignOutSucceeded()?.invoke()
                            } catch (e: Exception) {
                                L.e(tag, "Error invoking signOut callback", e)
                            }
                        }
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

    /** H5 请求登录（url 由 H5 给出，native 负责打开） */
    protected open fun getOnRequestLogin(): ((url: String?) -> Unit)? = null

    /** native 登出成功后的清理 */
    protected open fun getOnSignOutSucceeded(): (() -> Unit)? = null
}
