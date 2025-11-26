package co.candyhouse.app.ext.webview.bridge

import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.app.NotificationManagerCompat
import co.candyhouse.app.tabs.devices.hub3.bean.IrRemoteRepository
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.app.tabs.devices.ssm2.setNickname
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.CHResultState
import co.candyhouse.sesame.utils.L
import co.utils.SharedPreferencesUtils
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * 统一的WebView JS Bridge
 * 支持原生Fragment和Compose两种使用方式
 *
 * @author frey on 2025/10/9
 */
class WebViewJSBridge(
    private val webView: WebView?,
    private val scope: CoroutineScope,
    private val config: JSBridgeConfig = JSBridgeConfig()
) {

    private val tag = "WebViewJSBridge"

    data class JSBridgeConfig(
        val onHeightChanged: ((Float) -> Unit)? = null,
        val onRequestLogin: (() -> Unit)? = null,
        val onRequestNotificationSettings: (() -> Unit)? = null
    )

    @JavascriptInterface
    fun postMessage(message: String) {
        L.d(tag, "postMessage called with: $message")

        try {
            val json = JSONObject(message)
            val action = json.optString("action")
            val callbackName = json.optString("callbackName")

            L.d(tag, "Action: $action, Callback: $callbackName")

            when (action) {
                "requestDeviceList" -> {
                    handleRequestDeviceList(callbackName)
                }

                "requestDeviceName" -> {
                    val deviceUUID = json.optString("deviceUUID")
                    handleRequestDeviceName(callbackName, deviceUUID)
                }

                "requestDeviceRename" -> {
                    val deviceUUID = json.optString("deviceUUID")
                    val deviceName = json.optString("deviceName")
                    handleRequestDeviceRename(callbackName, deviceUUID, deviceName)
                }

                "requestDeviceInfo" -> {
                    val deviceUUID = json.optString("deviceUUID")
                    handleRequestDeviceInfo(callbackName, deviceUUID)
                }

                "requestAutoLayoutHeight" -> {
                    val height = json.optDouble("height", 0.0)
                    if (height > 0) {
                        scope.launch(Dispatchers.Main) {
                            config.onHeightChanged?.invoke(height.toFloat())
                        }
                    }
                }

                "requestLogin" -> {
                    scope.launch(Dispatchers.Main) {
                        config.onRequestLogin?.invoke()
                    }
                }

                "requestPushToken" -> {
                    SharedPreferencesUtils.deviceToken?.let { handleRequestPushToken(callbackName, it) }
                }

                "requestNotificationStatus" -> {
                    handleNotificationPermissionStatus(callbackName)
                }

                "requestNotificationSettings" -> {
                    scope.launch(Dispatchers.Main) {
                        config.onRequestNotificationSettings?.invoke()
                    }
                }

                "updateRemote" -> {
                    val hub3DeviceId = json.optString("hub3DeviceId")
                    val remoteId = json.optString("remoteId")
                    val alias = json.optString("alias")
                    updateRemote(hub3DeviceId, remoteId, alias)
                }

                else -> {
                    L.e(tag, "Unknown action: $action")
                }
            }
        } catch (e: Exception) {
            L.e(tag, "Error parsing message: ${e.message}")
        }
    }

    private fun updateRemote(hub3DeviceId: String, remoteId: String, alias: String) {
        val irRepository = IrRemoteRepository.getInstance()
        val list = irRepository.getRemotesByKey(hub3DeviceId)
        for (item in list) {
            if (item.uuid == remoteId) {
                item.alias = alias
            }
        }
        irRepository.setRemotes(hub3DeviceId,list)
    }

    private fun handleRequestDeviceList(callbackName: String) {
        scope.launch {
            CHDeviceManager.getCandyDevices { result ->
                result.onSuccess { chResultState ->
                    val responseData = when (chResultState) {
                        is CHResultState.CHResultStateBLE -> {
                            val devices = chResultState.data
                            L.d(tag, "Found ${devices.size} devices")

                            val deviceArray = JSONArray()
                            devices.forEach { device ->
                                val deviceJson = JSONObject().apply {
                                    put("deviceUUID", device.deviceId.toString().uppercase())
                                    put("deviceName", device.getNickname())
                                    put("deviceModel", device.productModel.deviceModel())
                                    put("keyLevel", device.getLevel().toString())
                                }
                                deviceArray.put(deviceJson)
                            }
                            deviceArray.toString()
                        }

                        else -> "[]"
                    }

                    sendResponseDataToH5(callbackName, responseData)
                }

                result.onFailure { error ->
                    L.e(tag, "Failed: ${error.message}")
                    scope.launch(Dispatchers.Main) {
                        webView?.evaluateJavascript("if(window.$callbackName) window.$callbackName([]);", null)
                    }
                }
            }
        }
    }

    private fun handleRequestDeviceName(callbackName: String, deviceUUID: String) {
        scope.launch {
            CHDeviceManager.getCandyDeviceByUUID(deviceUUID) { result ->
                result.onSuccess { chResultState ->
                    val responseData = when (chResultState) {
                        is CHResultState.CHResultStateBLE -> {
                            val device = chResultState.data

                            JSONObject().apply {
                                put(deviceUUID, device.getNickname())
                            }.toString()
                        }

                        else -> "{}"
                    }

                    sendResponseDataToH5(callbackName, responseData)
                }

                result.onFailure { error ->
                    L.e(tag, "Failed to get device name: ${error.message}")
                    scope.launch(Dispatchers.Main) {
                        webView?.evaluateJavascript("if(window.$callbackName) window.$callbackName({});", null)
                    }
                }
            }
        }
    }

    private fun handleRequestDeviceRename(callbackName: String, deviceUUID: String, deviceName: String) {
        scope.launch {
            CHDeviceManager.getCandyDeviceByUUID(deviceUUID) { result ->
                result.onSuccess { chResultState ->
                    val responseData = when (chResultState) {
                        is CHResultState.CHResultStateBLE -> {
                            val device = chResultState.data

                            device.setNickname(deviceName)

                            JSONObject().apply {
                                put("success", true)
                            }.toString()
                        }

                        else -> {
                            JSONObject().apply {
                                put("success", false)
                            }.toString()
                        }
                    }

                    sendResponseDataToH5(callbackName, responseData)
                }

                result.onFailure { error ->
                    L.e(tag, "Failed to rename device: ${error.message}")
                    scope.launch(Dispatchers.Main) {
                        val jsCode = """if(window.$callbackName) window.$callbackName({"success": false});"""
                        webView?.evaluateJavascript(jsCode, null)
                    }
                }
            }
        }
    }

    private fun handleRequestDeviceInfo(callbackName: String, deviceUUID: String) {
        scope.launch {
            CHDeviceManager.getCandyDeviceByUUID(deviceUUID) { result ->
                result.onSuccess { chResultState ->
                    val responseData = when (chResultState) {
                        is CHResultState.CHResultStateBLE -> {
                            val device = chResultState.data

                            val deviceKey = device.getKey()
                            val gson = Gson()
                            val jsonObj = run {
                                val jsonElement = gson.toJsonTree(deviceKey)
                                jsonElement.asJsonObject.apply {
                                    addProperty("keyLevel", device.getLevel())
                                }
                            }
                            jsonObj.toString()
                        }

                        else -> "{}"
                    }

                    sendResponseDataToH5(callbackName, responseData)
                }

                result.onFailure { error ->
                    L.e(tag, "Failed to get device name: ${error.message}")
                    scope.launch(Dispatchers.Main) {
                        webView?.evaluateJavascript("if(window.$callbackName) window.$callbackName({});", null)
                    }
                }
            }
        }
    }

    private fun handleRequestPushToken(callbackName: String, pushToken: String) {
        scope.launch {
            val responseData = JSONObject().apply {
                put("pushToken", pushToken)
            }

            sendResponseDataToH5(callbackName, responseData)
        }
    }

    private fun handleNotificationPermissionStatus(callbackName: String) {
        scope.launch {
            val isEnabled = NotificationManagerCompat.from(CHDeviceManager.app).areNotificationsEnabled()

            val responseData = JSONObject().apply {
                put("enabled", isEnabled)
            }

            sendResponseDataToH5(callbackName, responseData)
        }
    }

    private fun sendResponseDataToH5(callbackName: String, responseData: Any) {
        scope.launch(context = Dispatchers.Main) {
            val jsCode = "if(window.$callbackName) window.$callbackName($responseData);"
            webView?.evaluateJavascript(jsCode, null)
        }
    }
}