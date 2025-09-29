package co.candyhouse.app.tabs.friend

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import co.candyhouse.app.R
import co.candyhouse.app.databinding.FgFriendListBinding
import co.candyhouse.app.tabs.HomeFragment
import co.candyhouse.server.CHLoginAPIManager.getWebUrlByScene
import co.candyhouse.server.CHResultState
import co.candyhouse.sesame.utils.L
import co.utils.alertview.fragments.toastMSG
import co.utils.safeNavigate

class FriendListFG : HomeFragment<FgFriendListBinding>() {

    private val tag = "FriendListFG"
    private var webView: WebView? = null
    private var isRefreshing = false
    private var isManualRefresh = false

    override fun getViewBinder() = FgFriendListBinding.inflate(layoutInflater)

    override fun setupUI() {
        setupWebView()
    }

    override fun setupListeners() {
        bind.swiperefresh.setOnRefreshListener {
            if (!isRefreshing) {
                reloadFriends()
            }
        }
    }

    override fun <T : View> observeViewModelData(view: T) {}

    private fun setupWebView() {
        webView = ContactsWebViewManager.getOrCreateWebView(
            requireContext(),
            onSchemeIntercept = { uri, params ->
                handleSchemeIntercept(uri, params)
            },
            onPageStarted = {
                L.d(tag, "Page started loading")
            },
            onPageFinished = {
                L.d(tag, "Page finished loading")
                if (isAdded) {
                    setManualRefresh()
                }
            },
            onError = { error ->
                L.e(tag, "WebView error: $error")
                if (isAdded) {
                    bind.loadingProgress.visibility = View.GONE
                    setManualRefresh()
                }
            },
            onLoadingChanged = { isLoading ->
                if (isAdded && !isManualRefresh) {
                    bind.loadingProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
                }
            }
        )

        bind.webviewContainer.removeAllViews()
        bind.webviewContainer.addView(webView)

        if (ContactsWebViewManager.shouldReload()) {
            bind.loadingProgress.visibility = View.VISIBLE
            loadWebViewContent()
        } else {
            L.d(tag, "Using cached WebView, url=${ContactsWebViewManager.getCurrentUrl()}")
        }
    }

    private fun loadWebViewContent() {
        L.d(tag, "Loading WebView content")

        if (!isManualRefresh) {
            bind.loadingProgress.visibility = View.VISIBLE
        }

        // 获取URL
        getWebUrlByScene("contacts", null) { result ->
            if (!isAdded) {
                L.d(tag, "Fragment not attached, skip processing")
                return@getWebUrlByScene
            }
            result.fold(
                onSuccess = { state ->
                    val url = ((state as? CHResultState.CHResultStateNetworks)?.data ?: "") as String
                    if (url.isNotEmpty()) {
                        L.d(tag, "Got URL: $url")
                        activity?.runOnUiThread {
                            if (isAdded) {
                                ContactsWebViewManager.loadUrl(url)
                            }
                        }
                    } else {
                        activity?.runOnUiThread {
                            if (isAdded) {
                                bind.loadingProgress.visibility = View.GONE
                                setManualRefresh()
                            }
                        }
                    }
                },
                onFailure = { t ->
                    val errorMsg = t.message ?: "Load url failed"
                    L.e(tag, "Error: $errorMsg")
                    activity?.runOnUiThread {
                        if (isAdded) {
                            val message = "Please log in"
                            if (errorMsg.contains(message, ignoreCase = true)) {
                                toastMSG(message)
                            }
                            bind.loadingProgress.visibility = View.GONE
                            setManualRefresh()
                        }
                    }
                }
            )
        }
    }

    private fun handleSchemeIntercept(uri: Uri, params: Map<String, String>) {
        if (!isAdded) {
            L.d(tag, "Fragment not attached, skip scheme intercept")
            return
        }
        when (uri.path) {
            "/webview/open" -> {
                params["url"]?.let { targetUrl ->
                    params["notifyName"]?.let { notifyName ->
                        L.d(tag, "notifyName=$notifyName")
                    }
                    L.d(tag, "Opening URL: $targetUrl")
                    safeNavigate(
                        R.id.action_Friend_to_webViewFragment,
                        Bundle().apply {
                            putString("scene", "contacts")
                            putString("url", targetUrl)
                        }
                    )
                }
            }
        }
    }

    private fun reloadFriends() {
        if (isRefreshing) return

        isRefreshing = true
        isManualRefresh = true
        L.d(tag, "Manual refresh triggered")

        loadWebViewContent()
    }

    private fun setManualRefresh() {
        if (isManualRefresh) {
            bind.swiperefresh.isRefreshing = false
            isRefreshing = false
            isManualRefresh = false
        }
    }

    override fun onResume() {
        super.onResume()
        val needsRefresh = ContactsWebViewManager.checkAndConsumePendingRefresh()
        val pendingDetail = ContactsWebViewManager.checkAndConsumePendingDetail()

        if (needsRefresh) {
            if (pendingDetail != null) {
                loadWebViewContent()
            } else {
                doRefresh()
            }
        }

        pendingDetail?.let { detailBundle ->
            view?.postDelayed({
                hideAllLoadingIndicators()
                navigateToContactDetail(detailBundle)
            }, 300)
        }
    }

    override fun onPause() {
        super.onPause()
        hideAllLoadingIndicators()
    }

    private fun hideAllLoadingIndicators() {
        bind.swiperefresh.isRefreshing = false
        bind.loadingProgress.visibility = View.GONE
        isRefreshing = false
        isManualRefresh = false
    }


    private fun navigateToContactDetail(bundle: Bundle) {
        val scene = bundle.getString("scene", "")
        val email = bundle.getString("email", "")
        val subUUID = bundle.getString("subUUID", "")

        val extInfo = mapOf(
            "email" to email,
            "subUUID" to subUUID
        )

        getWebUrlByScene(scene, extInfo) { result ->
            result.fold(
                onSuccess = { state ->
                    val url = ((state as? CHResultState.CHResultStateNetworks)?.data ?: "") as String
                    if (url.isNotEmpty() && isAdded) {
                        activity?.runOnUiThread {
                            safeNavigate(
                                R.id.action_Friend_to_webViewFragment,
                                Bundle().apply {
                                    putString("scene", scene)
                                    putString("url", url)
                                }
                            )
                        }
                    }
                },
                onFailure = { t ->
                    L.e(tag, "Failed to get contact detail URL: ${t.message}")
                }
            )
        }
    }

    private fun doRefresh() {
        bind.swiperefresh.isRefreshing = true
        reloadFriends()
    }

    override fun onDestroyView() {
        isRefreshing = false
        isManualRefresh = false
        // 只从容器移除，不销毁WebView
        bind.webviewContainer.removeAllViews()
        webView = null
        super.onDestroyView()
    }

}