package co.candyhouse.app.tabs.friend

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.unit.Dp
import co.candyhouse.app.R
import co.candyhouse.app.databinding.FgFriendListBinding
import co.candyhouse.app.tabs.HomeFragment
import co.candyhouse.app.tabs.menu.EmbeddedWebView
import co.candyhouse.sesame.utils.L
import co.utils.safeNavigate

class FriendListFG : HomeFragment<FgFriendListBinding>() {

    private val refreshCounter = mutableIntStateOf(0)

    override fun getViewBinder() = FgFriendListBinding.inflate(layoutInflater)

    override fun setupUI() {
        setupWebView()
    }

    override fun setupListeners() {
        bind.swiperefresh.setOnRefreshListener {
            reloadFriends()
        }
    }

    override fun <T : View> observeViewModelData(view: T) {}

    private fun setupWebView() {
        bind.contactsWebView.apply {
            disposeComposition()

            setContent {
                EmbeddedWebView(
                    scene = "contacts",
                    height = Dp.Unspecified,
                    refreshTrigger = refreshCounter.intValue,
                    onSchemeIntercept = { uri, params ->
                        when (uri.path) {
                            "/webview/open" -> {
                                params["url"]?.let { targetUrl ->
                                    params["notifyName"]?.let { notifyName ->
                                        L.d("EmbeddedWebView", "notifyName=$notifyName")
                                    }

                                    L.d("EmbeddedWebView", "EmbeddedWebView-targetUrl=$targetUrl")
                                    safeNavigate(R.id.action_DeviceMember_to_webViewFragment, Bundle().apply {
                                        putString("scene", "contacts")
                                        putString("url", targetUrl)
                                    })
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    private fun reloadFriends() {
        // 触发WebView刷新
        refreshCounter.intValue++

        bind.swiperefresh.postDelayed({
            bind.swiperefresh.isRefreshing = false
        }, 1500)
    }

}