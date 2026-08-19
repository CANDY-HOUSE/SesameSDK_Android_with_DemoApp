package co.candyhouse.app.ext.webview.bridge

import android.content.Intent
import android.os.Build
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.ext.AppPromotionManager
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.server.dto.AppPromotion
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.SharedPreferencesUtils
import com.amplifyframework.auth.cognito.exceptions.service.UsernameExistsException
import com.amplifyframework.auth.result.step.AuthSignInStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    var hub3Bridge: Hub3JSBridge? = null

    data class JSBridgeConfig(
        val onHeightChanged: ((Float) -> Unit)? = null,
        val onRequestLogin: ((url: String?) -> Unit)? = null,
        val onRequestNotificationSettings: (() -> Unit)? = null,
        val onRequestDestroySelf: (() -> Unit)? = null,
        val onRequestRefreshApp: (() -> Unit)? = null,
        val onRequestWifiConfig: (() -> Unit)? = null,
        val onEnablePullRefresh: ((Boolean) -> Unit)? = null,
        val onRequestUpdateDeviceFWVersion: ((deviceId: String, currentFwVer: String) -> Unit)? = null,
        val onSignInSucceeded: (() -> Unit)? = null,
        val onSignOutSucceeded: (() -> Unit)? = null
    )

    companion object {
        const val requestRefreshApp = "requestRefreshApp"
        const val requestEnablePullRefresh = "requestEnablePullRefresh"
        const val requestDestroySelf = "requestDestroySelf"
        const val requestAutoLayoutHeight = "requestAutoLayoutHeight"
        const val requestLogin = "requestLogin"
        const val requestSignIn = "requestSignIn"
        const val requestConfirmSignIn = "requestConfirmSignIn"
        const val requestSignOut = "requestSignOut"
        const val requestAuthState = "requestAuthState"
        const val requestAppVersion = "requestAppVersion"
        const val requestOpenExternalURL = "requestOpenExternalURL"
        const val requestPushToken = "requestPushToken"
        const val requestNotificationStatus = "requestNotificationStatus"
        const val requestNotificationSettings = "requestNotificationSettings"
        const val requestActivePromotion = "requestActivePromotion"
        const val requestMarkPromotionRead = "requestMarkPromotionRead"
        const val requestBLEConnect = "requestBLEConnect"
        const val requestConfigureInternet = "requestConfigureInternet"
        const val requestMonitorInternet = "requestMonitorInternet"
        const val requestDeviceFWUpgrade = "requestDeviceFWUpgrade"
        const val requestUpdateDeviceFWVersion = "requestUpdateDeviceFWVersion"
    }

    @JavascriptInterface
    fun postMessage(message: String) {
        L.d(tag, "postMessage called with: $message")

        try {
            val json = JSONObject(message)
            val action = json.optString("action")
            val callbackName = json.optString("callbackName")

            L.d(tag, "Action: $action, Callback: $callbackName")

            when (action) {
                requestBLEConnect -> {
                    hub3Bridge?.handleRequestBLEConnect(json)
                }

                requestConfigureInternet -> {
                    hub3Bridge?.handleRequestConfigureInternet(json)
                }

                requestMonitorInternet -> {
                    hub3Bridge?.handleRequestMonitorInternet(json)
                }

                requestDeviceFWUpgrade -> {
                    hub3Bridge?.handleRequestDeviceFWUpgrade(json)
                }

                requestUpdateDeviceFWVersion -> {
                    handleRequestUpdateDeviceFWVersion(json)
                }

                requestRefreshApp -> {
                    scope.launch(Dispatchers.Main) {
                        config.onRequestRefreshApp?.invoke()
                    }
                }

                requestDestroySelf -> {
                    scope.launch(Dispatchers.Main) {
                        config.onRequestDestroySelf?.invoke()
                    }
                }

                requestEnablePullRefresh -> {
                    scope.launch(Dispatchers.Main) {
                        config.onEnablePullRefresh?.invoke(true)
                    }
                }

                requestAutoLayoutHeight -> {
                    val height = json.optDouble("height", 0.0)
                    if (height > 0) {
                        scope.launch(Dispatchers.Main) {
                            config.onHeightChanged?.invoke(height.toFloat())
                        }
                    }
                }

                requestLogin -> {
                    val loginUrl = json.optString("url").takeIf { it.isNotBlank() }
                    scope.launch(Dispatchers.Main) {
                        config.onRequestLogin?.invoke(loginUrl)
                    }
                }

                requestSignIn -> {
                    handleRequestSignIn(json, callbackName)
                }

                requestConfirmSignIn -> {
                    handleRequestConfirmSignIn(json, callbackName)
                }

                requestSignOut -> {
                    handleRequestSignOut(callbackName)
                }

                requestAuthState -> {
                    handleRequestAuthState(callbackName)
                }

                requestAppVersion -> {
                    handleRequestAppVersion(callbackName)
                }

                requestOpenExternalURL -> {
                    handleRequestOpenExternalURL(json)
                }

                requestPushToken -> {
                    SharedPreferencesUtils.deviceToken?.let { handleRequestPushToken(callbackName, it) }
                }

                requestNotificationStatus -> {
                    handleNotificationPermissionStatus(callbackName)
                }

                requestNotificationSettings -> {
                    scope.launch(Dispatchers.Main) {
                        config.onRequestNotificationSettings?.invoke()
                    }
                }

                requestActivePromotion -> {
                    handleRequestActivePromotion(callbackName)
                }

                requestMarkPromotionRead -> {
                    handleRequestMarkPromotionRead(json, callbackName)
                }

                else -> {
                    L.e(tag, "Unknown action: $action")
                }
            }
        } catch (e: Exception) {
            L.e(tag, "Error parsing message: ${e.message}")
        }
    }

    /** H5 传邮箱，native 触发 Cognito 发送验证码 */
    private fun handleRequestSignIn(json: JSONObject, callbackName: String) {
        val email = json.optString("email")
        if (email.isBlank()) {
            replyFailure(callbackName, "email required")
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                runCatching { AWSStatus.signUp(email) }
                    .onFailure { if (it !is UsernameExistsException) throw it }
                val result = AWSStatus.signIn(email)
                val needCode =
                    result.nextStep.signInStep == AuthSignInStep.CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE
                sendResponseDataToH5(callbackName, JSONObject().apply {
                    put("success", true)
                    put("needCode", needCode)
                })
            } catch (e: Exception) {
                L.e(tag, "signIn failed", e)
                replyFailure(callbackName, e.localizedMessage ?: "Sign in failed")
            }
        }
    }

    /** H5 传验证码，native 完成登录 */
    private fun handleRequestConfirmSignIn(json: JSONObject, callbackName: String) {
        val code = json.optString("code")
        if (code.isBlank()) {
            replyFailure(callbackName, "code required")
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                AWSStatus.confirmSignIn(code)
                val signedIn = AWSStatus.refreshAuthSessionNow()
                if (signedIn) {
                    updateNameIfNeeded()
                }
                sendResponseDataToH5(callbackName, JSONObject().apply {
                    put("success", true)
                    put("signedIn", signedIn)
                })
                if (signedIn) {
                    scope.launch(Dispatchers.Main) {
                        config.onSignInSucceeded?.invoke()
                    }
                }
            } catch (e: Exception) {
                L.e(tag, "confirmSignIn failed", e)
                replyFailure(callbackName, e.localizedMessage ?: "Incorrect username or password.")
            }
        }
    }

    /** 登录成功后：昵称为空时用邮箱前缀初始化，并记录 subUUID */
    private suspend fun updateNameIfNeeded() {
        runCatching {
            val attributes = AWSStatus.getUserAttributes()
            AWSStatus.setSubUUID(attributes["sub"])
            val name = attributes["name"]
            val email = attributes["email"]
            if (name.isNullOrEmpty() && !email.isNullOrEmpty()) {
                val nickname = email.split("@").firstOrNull() ?: ""
                if (nickname.isNotEmpty()) {
                    AWSStatus.updateUserName(nickname)
                }
            }
        }.onFailure { L.e(tag, "updateNameIfNeeded failed: ${it.message}") }
    }

    /** 登出（确认交互由 H5 处理，native 只执行登出） */
    private fun handleRequestSignOut(callbackName: String) {
        scope.launch(Dispatchers.IO) {
            try {
                AWSStatus.signOut()
                sendResponseDataToH5(callbackName, JSONObject().apply { put("success", true) })
                scope.launch(Dispatchers.Main) {
                    config.onSignOutSucceeded?.invoke()
                }
            } catch (e: Exception) {
                L.e(tag, "signOut failed", e)
                replyFailure(callbackName, e.localizedMessage ?: "Sign out failed")
            }
        }
    }

    /** 登录态查询 */
    private fun handleRequestAuthState(callbackName: String) {
        if (callbackName.isBlank()) return
        sendResponseDataToH5(callbackName, JSONObject().apply {
            put("signedIn", AWSStatus.isSignedIn())
            put("state", AWSStatus.getCachedUserState().name)
        })
    }

    /** App 版本号 */
    private fun handleRequestAppVersion(callbackName: String) {
        if (callbackName.isBlank()) return
        val display = BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + ")" +
                "-" + BuildConfig.GIT_HASH + "-" + BuildConfig.BUILD_TYPE +
                "-" + Build.MODEL + ":" + Build.VERSION.SDK_INT
        sendResponseDataToH5(callbackName, JSONObject().apply { put("display", display) })
    }

    /** 外部浏览器打开 URL */
    private fun handleRequestOpenExternalURL(json: JSONObject) {
        val url = json.optString("url")
        if (url.isBlank()) return
        val context = webView?.context ?: return
        scope.launch(Dispatchers.Main) {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }.onFailure { L.e(tag, "openExternalURL failed: $url", it) }
        }
    }

    private fun replyFailure(callbackName: String, message: String) {
        if (callbackName.isBlank()) return
        sendResponseDataToH5(callbackName, JSONObject().apply {
            put("success", false)
            put("error", message)
        })
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

    private fun handleRequestActivePromotion(callbackName: String) {
        if (callbackName.isBlank()) return

        AppPromotionManager.refresh { promotion ->
            sendResponseDataToH5(callbackName, promotion.toResponseJson())
        }
    }

    private fun handleRequestMarkPromotionRead(json: JSONObject, callbackName: String) {
        val promotionId = json.optString("promotionId")
        val targetUrl = json.optString("targetUrl").takeIf { it.isNotBlank() }
        if (promotionId.isBlank()) {
            if (callbackName.isNotBlank()) {
                sendResponseDataToH5(callbackName, JSONObject().apply {
                    put("success", false)
                })
            }
            return
        }

        AppPromotionManager.markRead(promotionId, targetUrl) { promotion ->
            if (callbackName.isNotBlank()) {
                sendResponseDataToH5(callbackName, promotion.toResponseJson())
            }
        }
    }

    private fun handleRequestUpdateDeviceFWVersion(json: JSONObject) {
        val deviceId = json.optString("deviceUUID")
        val currentFwVer = json.optString("currentFwVer")

        if (deviceId.isBlank() || currentFwVer.isBlank()) return

        scope.launch(Dispatchers.Main) {
            config.onRequestUpdateDeviceFWVersion?.invoke(deviceId, currentFwVer)
        }
    }

    private fun sendResponseDataToH5(callbackName: String, responseData: Any) {
        scope.launch(context = Dispatchers.Main) {
            val jsCode = "if(window.$callbackName) window.$callbackName($responseData);"
            webView?.evaluateJavascript(jsCode, null)
        }
    }

    private fun AppPromotion?.toResponseJson(): JSONObject {
        return JSONObject().apply {
            put("success", this@toResponseJson != null)
            this@toResponseJson?.let { promotion ->
                put("promotionId", promotion.promotionId)
                put("enabled", promotion.enabled)
                put("visible", promotion.visible)
                put("targetUrl", promotion.targetUrl)
            }
        }
    }

    fun cleanup() {
        hub3Bridge?.cleanup()
        hub3Bridge = null
    }
}
