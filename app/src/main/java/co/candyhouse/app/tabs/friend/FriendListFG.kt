package co.candyhouse.app.tabs.friend

import android.net.Uri
import android.os.Bundle
import android.view.View
import co.candyhouse.app.R
import co.candyhouse.app.databinding.FgFriendListBinding
import co.candyhouse.app.ext.webview.BaseNativeWebViewFragment
import co.utils.safeNavigate

class FriendListFG : BaseNativeWebViewFragment<FgFriendListBinding>() {

    override val webViewName = "contacts"

    override fun getViewBinder() = FgFriendListBinding.inflate(layoutInflater)
    override fun getWebViewContainer() = bind.friendWebviewContainer
    override fun getLoadingView() = bind.friendLoadingProgress
    override fun getSwipeRefreshLayout() = bind.swiperefresh

    override fun handleSchemeIntercept(uri: Uri, params: Map<String, String>) {
        when (uri.path) {
            "/webview/open" -> {
                params["url"]?.let { targetUrl ->
                    safeNavigate(
                        R.id.action_to_webViewFragment,
                        Bundle().apply {
                            putString("scene", webViewName)
                            putString("url", targetUrl)
                        }
                    )
                }
            }
        }
    }

    override fun <T : View> observeViewModelData(view: T) {}
}