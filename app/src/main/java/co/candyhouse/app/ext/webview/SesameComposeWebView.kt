package co.candyhouse.app.ext.webview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import co.candyhouse.app.R
import co.candyhouse.app.ext.webview.bridge.WebViewJSBridge
import co.candyhouse.app.ext.webview.data.WebViewConfig
import co.candyhouse.app.ext.webview.manager.WebViewPoolManager
import co.candyhouse.app.ext.webview.util.SesameComposeWebViewContent
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.sesame.utils.L
import co.utils.ContainerPaddingManager
import co.utils.safeNavigate

class SesameComposeWebView : Fragment() {

    private val logTag = "SesameComposeWebView"

    private val mDeviceModel: CHDeviceViewModel by activityViewModels()
    private var currentScene: String = ""
    private var wifiModuleJsBridge: WebViewJSBridge? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            val config = WebViewConfig.fromArguments(arguments)
            LaunchedEffect(config.scene) {
                currentScene = config.scene
            }
            SesameComposeWebViewContent(
                config = config,
                onBackClick = {
                    exit()
                },
                onMoreClick = { whereValue ->
                    when (whereValue) {
                        "device_history_old" -> safeNavigate(R.id.action_mainRoomFG_to_SSM2SettingFG)
                        "device_history_new" -> safeNavigate(R.id.action_mainRoomSS5FG_to_SSM5SettingFG)
                    }
                },
                onSchemeIntercept = { uri, params ->
                    when (uri.path) {
                        "/webview/notify" -> {
                            params["notifyName"]?.let { notifyName ->
                                L.e(logTag, "notifyName=$notifyName")
                                when (notifyName) {
                                    "FriendChanged" -> {
                                        WebViewPoolManager.setPendingRefresh("contacts")
                                        findNavController().popBackStack()
                                    }

                                    "RefreshList" -> {
                                        WebViewPoolManager.setPendingRefresh("contacts")
                                    }

                                    "UserProfileChanged" -> {
                                        WebViewPoolManager.setPendingRefresh("me-index")
                                    }
                                }
                            }
                        }
                    }
                },
                onRequestWifiConfig = {
                    safeNavigate(R.id.to_HUB3ScanSSIDListFG)
                },
                onRequestRefreshApp = {
                    mDeviceModel.refreshDevices()
                },
                deviceModel = mDeviceModel,
                onJSBridgeCreated = { bridge ->
                    if (config.scene == "wifi-module") {
                        wifiModuleJsBridge?.cleanup()
                        wifiModuleJsBridge = bridge
                    }
                }
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ContainerPaddingManager.requestClearPadding(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ContainerPaddingManager.releaseClearPadding(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (currentScene == "wifi-module") {
            wifiModuleJsBridge?.cleanup()
            wifiModuleJsBridge = null
        }
    }

    private fun exit() {
        if (!findNavController().popBackStack()) {
            requireActivity().finish()
        }
    }
}