package co.utils

import android.webkit.JavascriptInterface
import android.webkit.WebView
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.CHResultState
import co.candyhouse.sesame.utils.L
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
    private val scope: CoroutineScope
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
                                    put("keyLevel", device.getLevel())
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
}