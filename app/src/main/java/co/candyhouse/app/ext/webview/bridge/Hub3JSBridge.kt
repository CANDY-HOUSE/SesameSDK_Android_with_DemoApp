package co.candyhouse.app.ext.webview.bridge

import android.content.Context
import android.webkit.WebView
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.tabs.devices.ssm2.localizedDescription
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.open.device.CHHub3Delegate
import co.candyhouse.sesame.open.device.CHWifiModule2
import co.candyhouse.sesame.open.device.CHWifiModule2MechSettings
import co.candyhouse.sesame.open.device.CHWifiModule2NetWorkStatus
import co.candyhouse.sesame.open.device.NSError
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume

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

    private var statusCallbacks = mutableMapOf<String, String>()
    private var currentDeviceUUID: String? = null
    private var registeredDeviceForDelegateMap: CHHub3? = null
    private var pendingApSetting: CHWifiModule2MechSettings? = null
    private var pendingMechStatus: CHWifiModule2NetWorkStatus? = null
    private var connectJob: Job? = null
    private var isConnecting = false

    /**
     * 处理蓝牙连接请求
     */
    fun handleRequestBLEConnect(json: JSONObject) {
        val callbackName = json.optString("callbackName")
        val deviceUUID = json.optString("deviceUUID")

        statusCallbacks[WebViewJSBridge.requestBLEConnect] = callbackName
        currentDeviceUUID = deviceUUID

        scope.launch {
            val device = getCurrentDevice(deviceUUID)
            if (device == null) {
                L.e(tag, "Device not found")
                sendBLEStatusToH5(callbackName, "Device not found")
                return@launch
            }

            deviceModel?.ssmosLockDelegates?.set(device, this@Hub3JSBridge)
            registeredDeviceForDelegateMap = device

            if (device.deviceStatus.value == CHDeviceLoginStatus.logined) {
                L.i(tag, "蓝牙已连接……${device.deviceStatus.value}")
                sendBLEStatusToH5(callbackName, device.deviceStatus.value.toString())
                return@launch
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
                    return@launch
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
        scope.launch {
            currentDeviceUUID?.let { uuid ->
                val device = getCurrentDevice(uuid) ?: return@launch
                device.updateFirmwareBleOnly {}
            }
        }
    }

    /**
     * 清理连接
     */
    fun cleanup() {
        connectJob?.cancel()
        connectJob = null
        isConnecting = false
        registeredDeviceForDelegateMap?.let { dev ->
            deviceModel?.ssmosLockDelegates?.remove(dev)
        }
        registeredDeviceForDelegateMap = null
        currentDeviceUUID?.let { uuid ->
            disconnectDevice(uuid)
        }
        statusCallbacks.clear()
        currentDeviceUUID = null
        pendingMechStatus = null
        pendingApSetting = null
    }

    override fun onBleDeviceStatusChanged(
        device: CHDevices,
        status: CHDeviceStatus,
        shadowStatus: CHDeviceStatus?
    ) {
        if (status == CHDeviceStatus.ReceivedAdV) {
            if (isConnecting) {
                L.d(tag, "Already connecting, skip")
                return
            }
            connectJob?.cancel()
            connectJob = scope.launch {
                delay(1000)
                isConnecting = true
                device.connect { result ->
                    isConnecting = false
                    result.onSuccess {
                        L.d(tag, "ReceivedAdV connect Success")
                    }
                    result.onFailure { error ->
                        L.e(tag, "ReceivedAdV connect Failure = ${error.message}")
                    }
                }
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
                put("deviceUUID", device.deviceId.toString().uppercase())
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

    private suspend fun getCurrentDevice(uuid: String): CHHub3? {
        return suspendCancellableCoroutine { continuation ->
            CHDeviceManager.getCandyDeviceByUUID(uuid) { result ->
                result.onSuccess { state ->
                    val device = (state as? CHResultState.CHResultStateBLE)?.data as? CHHub3
                    continuation.resume(device)
                }
                result.onFailure {
                    continuation.resume(null)
                }
            }
        }
    }

    private fun disconnectDevice(uuid: String) {
        L.d(tag, "disconnectDevice:$uuid")
        scope.launch(NonCancellable) {
            getCurrentDevice(uuid)?.disconnect { result ->
                result.onSuccess {
                    L.d(tag, "disconnect Success")
                }
                result.onFailure { error ->
                    L.e(tag, "disconnect Failure = ${error.message}")
                }
            }
        }
    }
}