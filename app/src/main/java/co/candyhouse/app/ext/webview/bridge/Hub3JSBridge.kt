package co.candyhouse.app.ext.webview.bridge

import android.content.Context
import android.webkit.WebView
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.tabs.devices.ssm2.localizedDescription
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.open.device.CHHub3Delegate
import co.candyhouse.sesame.open.device.CHWifiModule2
import co.candyhouse.sesame.open.device.CHWifiModule2MechSettings
import co.candyhouse.sesame.open.device.CHWifiModule2NetWorkStatus
import co.candyhouse.sesame.open.device.NSError
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Hub3桥接
 *
 * @author frey on 2025/12/26
 */
class Hub3JSBridge(
    private val webView: WebView?,
    private val scope: CoroutineScope,
    private val context: Context,
    private val deviceModel: CHDeviceViewModel? = null,
    private val onRequestWifiConfig: (() -> Unit)? = null
) : CHHub3Delegate {

    private val tag = "Hub3JSBridge"

    // 存储回调和设备信息
    private var statusCallbacks = mutableMapOf<String, String>()
    private var currentDeviceUUID: String? = null
    private var currentDevice: CHHub3? = null
    private var registeredDeviceForDelegateMap: CHHub3? = null
    private var pendingApSetting: CHWifiModule2MechSettings? = null
    private var pendingMechStatus: CHWifiModule2NetWorkStatus? = null

    /**
     * 处理蓝牙连接请求
     */
    fun handleRequestBLEConnect(json: JSONObject) {
        val callbackName = json.optString("callbackName")
        val deviceUUID = json.optString("deviceUUID")

        statusCallbacks[WebViewJSBridge.requestBLEConnect] = callbackName
        currentDeviceUUID = deviceUUID

        scope.launch {
            CHDeviceManager.getCandyDeviceByUUID(deviceUUID) { result ->
                result.onSuccess { chResultState ->
                    when (chResultState) {
                        is CHResultState.CHResultStateBLE -> {
                            val device = chResultState.data as? CHHub3 ?: return@onSuccess
                            currentDevice = device

                            deviceModel?.ssmosLockDelegates?.set(device, this@Hub3JSBridge)
                            registeredDeviceForDelegateMap = device

                            if (device.deviceStatus.value == CHDeviceLoginStatus.logined) {
                                sendBLEStatusToH5(callbackName, device.deviceStatus.value.toString())
                                return@onSuccess
                            }

                            var lastNSError: NSError? = null
                            val okToProceed = device.isBleAvailable<CHDevices> { r ->
                                r.onFailure { e ->
                                    lastNSError = e as? NSError
                                }
                            }

                            if (!okToProceed) {
                                val code = lastNSError?.code
                                val isUnlogin = (code == -1)
                                if (!isUnlogin) {
                                    sendBLEStatusToH5(callbackName, context.getString(R.string.noble))
                                    return@onSuccess
                                }
                            }

                            device.connect {
                                it.onSuccess {
                                    L.d(tag, "connect Success")
                                }
                                it.onFailure { error ->
                                    L.e(tag, "connect Failure = ${error.message.toString()}")
                                    sendBLEStatusToH5(callbackName, context.getString(R.string.NoBleSignal))
                                }
                            }
                        }

                        else -> {
                            L.e(tag, "Device not found or wrong type")
                        }
                    }
                }

                result.onFailure { error ->
                    L.e(tag, "Failed to get device: ${error.message}")
                    sendBLEStatusToH5(callbackName, "Device not found")
                }
            }
        }
    }

    /**
     * 处理网络状态监控请求
     */
    fun handleRequestMonitorInternet(json: JSONObject) {
        val callbackName = json.optString("callbackName")
        statusCallbacks[WebViewJSBridge.requestMonitorInternet] = callbackName
        L.d(tag, "Start monitoring internet: $callbackName")

        pendingApSetting?.let { sendAPSettingToH5(callbackName, it) }
        pendingMechStatus?.let { sendNetworkStatusToH5(callbackName, "onMechStatus", it) }
    }

    /**
     * 处理 WiFi 配置请求
     */
    fun handleRequestConfigureInternet(json: JSONObject) {
        scope.launch(Dispatchers.Main) {
            onRequestWifiConfig?.invoke()
        }
    }

    /**
     * 处理蓝牙状态下OTA升级
     */
    fun handleRequestDeviceFWUpgrade(json: JSONObject) {
        val callbackName = json.optString("callbackName")
        statusCallbacks[WebViewJSBridge.requestDeviceFWUpgrade] = callbackName
        val device = currentDevice ?: return
        device.updateFirmwareBleOnly {}
    }

    /**
     * 清理连接
     */
    fun cleanup() {
        registeredDeviceForDelegateMap?.let { dev ->
            deviceModel?.ssmosLockDelegates?.remove(dev)
        }
        registeredDeviceForDelegateMap = null
        currentDevice?.disconnect { }
        currentDevice = null
        statusCallbacks.clear()
        currentDeviceUUID = null
    }

    override fun onBleDeviceStatusChanged(
        device: CHDevices,
        status: CHDeviceStatus,
        shadowStatus: CHDeviceStatus?
    ) {
        if (status == CHDeviceStatus.ReceivedAdV) {
            scope.launch {
                delay(1000)
                device.connect { }
            }
        }
        val callbackName = statusCallbacks[WebViewJSBridge.requestBLEConnect]
        if (callbackName != null) {
            val statusText = if (device.deviceStatus.value == CHDeviceLoginStatus.logined) {
                device.deviceStatus.value.toString()
            } else {
                device.localizedDescription(context) ?: status.toString()
            }
            sendBLEStatusToH5(callbackName, statusText)
        }
    }

    override fun onMechStatus(device: CHDevices) {
        if (device !is CHHub3) return
        val status = device.mechStatus as? CHWifiModule2NetWorkStatus ?: return

        pendingMechStatus = status

        val callbackName = statusCallbacks[WebViewJSBridge.requestMonitorInternet]
        if (!callbackName.isNullOrEmpty()) {
            sendNetworkStatusToH5(callbackName, "onMechStatus", status)
        }
    }

    override fun onAPSettingChanged(device: CHWifiModule2, settings: CHWifiModule2MechSettings) {
        pendingApSetting = settings

        val callbackName = statusCallbacks[WebViewJSBridge.requestMonitorInternet]
        if (!callbackName.isNullOrEmpty()) {
            sendAPSettingToH5(callbackName, settings)
        }
    }

    override fun onSSM2KeysChanged(device: CHWifiModule2, ssm2keys: Map<String, String>) {
        L.d(tag, "onSSM2KeysChanged: $ssm2keys")
    }

    override fun onOTAProgress(device: CHWifiModule2, percent: Byte) {
        val cbName = statusCallbacks[WebViewJSBridge.requestDeviceFWUpgrade] ?: return

        val p = percent.toInt() and 0xFF
        if (p % 10 != 0) return

        scope.launch(Dispatchers.Main) {
            val jsonData = JSONObject().apply {
                put("deviceUUID", device.deviceId.toString())
                put("percent", p.toString())
            }

            val jsCode = """
            if(window.$cbName) {
                window.$cbName($jsonData);
            }
        """.trimIndent()

            webView?.evaluateJavascript(jsCode, null)
        }
    }

    private fun sendBLEStatusToH5(callbackName: String, status: String) {
        scope.launch(Dispatchers.Main) {
            val jsCode = """
                if(window.$callbackName) {
                    window.$callbackName({bleStatus: "$status"});
                }
            """.trimIndent()
            webView?.evaluateJavascript(jsCode, null)
        }
    }

    private fun sendNetworkStatusToH5(
        callbackName: String,
        op: String,
        status: CHWifiModule2NetWorkStatus
    ) {
        scope.launch(Dispatchers.Main) {
            val jsonData = JSONObject().apply {
                put("op", op)
                put("isAPWork", status.isAPWork == true)
                put("isNetwork", status.isNetWork == true)
                put("isIoTWork", status.isIOTWork == true)
                put("isBindingAPWork", status.isAPConnecting)
                put("isConnectingNetwork", status.isConnectingNet)
                put("isConnectingIoT", status.isConnectingIOT)
            }

            val jsCode = """
                if(window.$callbackName) {
                    window.$callbackName($jsonData);
                }
            """.trimIndent()
            webView?.evaluateJavascript(jsCode, null)
        }
    }

    private fun sendAPSettingToH5(callbackName: String, settings: CHWifiModule2MechSettings) {
        scope.launch(Dispatchers.Main) {
            val jsonData = JSONObject().apply {
                put("op", "onAPSettingChanged")
                put("wifiSsid", settings.wifiSSID ?: "")
                put("wifiPwd", settings.wifiPassWord ?: "")
            }

            val jsCode = """
                if(window.$callbackName) {
                    window.$callbackName($jsonData);
                }
            """.trimIndent()
            webView?.evaluateJavascript(jsCode, null)
        }
    }
}