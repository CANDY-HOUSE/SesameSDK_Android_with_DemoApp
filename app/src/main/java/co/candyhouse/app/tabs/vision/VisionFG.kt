package co.candyhouse.app.tabs.vision

import android.net.Uri
import android.os.Bundle
import android.view.View
import co.candyhouse.app.R
import co.candyhouse.app.databinding.FgVisionBinding
import co.candyhouse.app.ext.webview.BaseNativeWebViewFragment
import co.utils.safeNavigate

class VisionFG : BaseNativeWebViewFragment<FgVisionBinding>() {

    override val webViewName = "vision"

    override fun getViewBinder() = FgVisionBinding.inflate(layoutInflater)
    override fun getWebViewContainer() = bind.visionWebviewContainer
    override fun getLoadingView() = bind.visionLoadingProgress
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
