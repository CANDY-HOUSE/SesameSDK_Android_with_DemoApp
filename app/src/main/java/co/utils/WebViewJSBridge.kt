package co.utils

import android.webkit.JavascriptInterface
import android.webkit.WebView
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.app.tabs.devices.ssm2.setNickname
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.CHResultState
import co.candyhouse.sesame.utils.L
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * JS桥接
 *
 * @author frey on 2025/10/9
 */
class WebViewJSBridge(
    private val webView: WebView?,
    private val scope: CoroutineScope,
    private val onHeightChanged: ((Float) -> Unit)? = null
) {

    private val tag = "WebViewJSBridge"

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
                            onHeightChanged?.invoke(height.toFloat())
                        }
                    }
                }

                else -> {
                    L.e(tag, "Unknown action: $action")
                }
            }
        } catch (e: Exception) {
            L.e(tag, "Error parsing message: ${e.message}")
        }
    }

    private fun handleRequestDeviceList(callbackName: String) {
        scope.launch {
            CHDeviceManager.getCandyDevices { result ->
                result.onSuccess { chResultState ->
                    val deviceList = when (chResultState) {
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

                    scope.launch(Dispatchers.Main) {
                        val jsCode = "if(window.$callbackName) window.$callbackName($deviceList);"
                        webView?.evaluateJavascript(jsCode, null)
                    }
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
            CHDeviceManager.getCandyDevices { result ->
                result.onSuccess { chResultState ->
                    val responseData = when (chResultState) {
                        is CHResultState.CHResultStateBLE -> {
                            val devices = chResultState.data
                            val device = devices.find {
                                it.deviceId.toString().equals(deviceUUID, ignoreCase = true)
                            }

                            if (device != null) {
                                JSONObject().apply {
                                    put(deviceUUID, device.getNickname())
                                }.toString()
                            } else {
                                "{}"
                            }
                        }

                        else -> "{}"
                    }

                    scope.launch(Dispatchers.Main) {
                        val jsCode = "if(window.$callbackName) window.$callbackName($responseData);"
                        webView?.evaluateJavascript(jsCode, null)
                    }
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
            CHDeviceManager.getCandyDevices { result ->
                result.onSuccess { chResultState ->
                    val responseData = when (chResultState) {
                        is CHResultState.CHResultStateBLE -> {
                            val devices = chResultState.data
                            val device = devices.find {
                                it.deviceId.toString().equals(deviceUUID, ignoreCase = true)
                            }

                            if (device != null) {
                                device.setNickname(deviceName)

                                JSONObject().apply {
                                    put("success", true)
                                }.toString()
                            } else {
                                JSONObject().apply {
                                    put("success", false)
                                }.toString()
                            }
                        }

                        else -> {
                            JSONObject().apply {
                                put("success", false)
                            }.toString()
                        }
                    }

                    scope.launch(Dispatchers.Main) {
                        val jsCode = "if(window.$callbackName) window.$callbackName($responseData);"
                        webView?.evaluateJavascript(jsCode, null)
                    }
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
            CHDeviceManager.getCandyDevices { result ->
                result.onSuccess { chResultState ->
                    val responseData = when (chResultState) {
                        is CHResultState.CHResultStateBLE -> {
                            val devices = chResultState.data
                            val device = devices.find {
                                it.deviceId.toString().equals(deviceUUID, ignoreCase = true)
                            }

                            if (device != null) {
                                val deviceKey = device.getKey()
                                val gson = Gson()
                                val jsonObj = run {
                                    val jsonElement = gson.toJsonTree(deviceKey)
                                    jsonElement.asJsonObject.apply {
                                        addProperty("keyLevel", device.getLevel())
                                    }
                                }
                                jsonObj.toString()
                            } else {
                                "{}"
                            }
                        }

                        else -> "{}"
                    }

                    scope.launch(Dispatchers.Main) {
                        val jsCode = "if(window.$callbackName) window.$callbackName($responseData);"
                        webView?.evaluateJavascript(jsCode, null)
                    }
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

}