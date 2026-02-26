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
import co.candyhouse.app.tabs.devices.hub3.setting.Hub3ScanSSIDDialogFragment
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
                        "device_history_ss2_lock" -> safeNavigate(R.id.action_mainRoomFG_to_SSM2SettingFG)
                        "device_history_ss5_lock" -> safeNavigate(R.id.action_mainRoomSS5FG_to_SSM5SettingFG)
                        "device_history_bike" -> safeNavigate(R.id.action_deviceListPG_to_sesameBikeSettingFG)
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
                    showWifiScanDialog()
                },
                onRequestRefreshApp = {
                    mDeviceModel.refreshDevices()
                },
                deviceModel = mDeviceModel,
                onJSBridgeCreated = { bridge ->
                    if (config.scene == "wifi-module") {
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

    private fun showWifiScanDialog() {
        if (childFragmentManager.findFragmentByTag(Hub3ScanSSIDDialogFragment.TAG) != null) {
            return
        }
        Hub3ScanSSIDDialogFragment.newInstance().show(childFragmentManager, Hub3ScanSSIDDialogFragment.TAG)
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