package co.candyhouse.app.tabs.account

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.R
import co.candyhouse.app.databinding.FgMeBinding
import co.candyhouse.app.ext.CHDeviceWrapperManager
import co.candyhouse.app.ext.webview.BaseNativeWebViewFragment
import co.candyhouse.app.ext.webview.manager.WebViewPoolManager
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.tabs.devices.model.CHLoginViewModel
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.device.CHSesameLock
import co.candyhouse.sesame.utils.L
import co.receiver.widget.SesameForegroundService
import co.utils.GuestUploadFlag
import co.utils.SharedPreferencesUtils
import co.utils.UserUtils
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.objects.AlertAction
import co.utils.safeNavigate
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.UserState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MeFG : BaseNativeWebViewFragment<FgMeBinding>() {

    private val tag = "MeFG"
    override val webViewName = "me-index"
    override fun getViewBinder() = FgMeBinding.inflate(layoutInflater)
    override fun getWebViewContainer() = bind.meWebviewContainer
    override fun getLoadingView() = bind.meLoadingProgress
    override fun getSwipeRefreshLayout() = bind.meRefresh

    private val loginViewModel: CHLoginViewModel by activityViewModels()
    private val deviceViewModel: CHDeviceViewModel by activityViewModels()

    override fun setupCustomUI() {
        bind.version.apply {
            val text =
                BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + ")" + "-" + BuildConfig.GIT_HASH + "-" + BuildConfig.BUILD_TYPE + "-" + Build.MODEL + ":" + Build.VERSION.SDK_INT
            val spannable = SpannableString(text)
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/CANDY-HOUSE/SesameSDK_Android_with_DemoApp/releases/latest/download/Sesame_android_release.apk".toUri()
                        )
                    )
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                    ds.color = Color.GRAY
                }
            }
            spannable.setSpan(clickableSpan, 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            this.text = spannable
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    override fun setupCustomListeners() {
        bind.logoutZone.setOnClickListener {
            // 登出
            showLogoutConfirmation(0)
        }
        bind.delAccount.setOnClickListener {
            // 删除
            showLogoutConfirmation(1)
        }
    }

    override fun <T : View> observeViewModelData(view: T) {
        viewLifecycleOwner.lifecycleScope.launch {
            loginViewModel.gUserState.collect { loginState ->
                updateUIForLoginState(loginState)
            }
        }
    }

    override fun handleSchemeIntercept(uri: Uri, params: Map<String, String>) {
        L.e(tag, "uri=$uri")
        when (uri.path) {
            "/webview/open" -> {
                params["url"]?.let { targetUrl ->
                    val scene = if (targetUrl.contains("device-notify")) {
                        "device-notify"
                    } else {
                        "me"
                    }

                    safeNavigate(
                        actionId = R.id.action_to_webViewFragment,
                        Bundle().apply {
                            putString("scene", scene)
                            putString("url", targetUrl)
                        }
                    )
                }
            }
        }
    }

    override fun getOnRequestLogin(): () -> Unit = {
        safeNavigate(R.id.action_register_to_LoginMailFG)
    }

    private fun showLogoutConfirmation(event: Int) {
        // 退出登录确认对话框逻辑
        val title: String = if (event == 0) {
            getString(R.string.logout)
        } else {
            getString(R.string.delete_account)
        }
        AlertView(title, "", AlertStyle.IOS).apply {
            addAction(
                AlertAction(
                    "OK",
                    AlertActionStyle.NEGATIVE
                ) { _ ->
                    performLogout()
                })
            show(activity as AppCompatActivity)
        }
    }

    @SuppressLint("ImplicitSamInstance")
    private fun performLogout() {
        // 执行退出登录操作
        AWSMobileClient.getInstance().signOut()
        bind.scrollView.scrollTo(0, 0)

        CHDeviceManager.getCandyDevices { result ->
            result.onSuccess { devices ->
                devices.data.forEach { device ->
                    when (device) {
                        is CHSesameLock -> {
                            NotificationManagerCompat.from(CHDeviceManager.app).cancel(device.deviceId.hashCode())
                            deviceViewModel.unregisterNotification(device)
                        }
                    }
                }
                CHDeviceManager.dropAllKeys(devices.data) {
                    deviceViewModel.updateDevices()
                }
            }
        }

        NotificationManagerCompat.from(CHDeviceManager.app).cancel("all".hashCode())
        if (SesameForegroundService.isLive) {
            CHDeviceManager.app.stopService(Intent(CHDeviceManager.app, SesameForegroundService::class.java))
        }
        CHDeviceWrapperManager.clear()
        GuestUploadFlag.clear()
        WebViewPoolManager.clearWebView("contacts")
        reloadRefresh()
    }

    private fun updateUIForLoginState(loginState: UserState) {
        bind.loginStateTxt.text = loginState.name

        if (loginState == UserState.SIGNED_IN) {
            handleSignedInState()
        } else {
            handleSignedOutState()
        }
    }

    private fun handleSignedInState() {
        bind.logoutZone.visibility = View.VISIBLE
        bind.delAccount.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            loadUserNickname()
            UserUtils.loadUserUserId()
        }
    }

    private fun handleSignedOutState() {
        bind.logoutZone.visibility = View.GONE
        bind.delAccount.visibility = View.GONE
    }

    private fun loadUserNickname() {
        runCatching {
            val nickname = AWSMobileClient.getInstance().getUserAttributes()["nickname"]
            L.d(tag, "nickname=$nickname")
            SharedPreferencesUtils.nickname = nickname
        }.onFailure { e ->
            L.e(tag, "${e.message}")
        }
    }
}

fun cheyKeyToUserKey(key: CHDevice, level: Int, nickName: String, rank: Int? = null): CHUserKey {
    return CHUserKey(
        key.deviceUUID,
        key.deviceModel,
        key.keyIndex,
        key.secretKey,
        key.sesame2PublicKey,
        nickName,
        level,
        rank
    )
}

fun userKeyToCHKey(key: CHUserKey, historyTag: ByteArray? = null): CHDevice {
    val deviceModel = key.deviceModel

    return CHDevice(
        key.deviceUUID,
        deviceModel,
        historyTag,
        key.keyIndex,
        key.secretKey,
        key.sesame2PublicKey
    )
}

fun getHistoryTag(): ByteArray {
    return SharedPreferencesUtils.nickname?.toByteArray() ?: CHDeviceManager.app.getString(R.string.unLoginHistoryTag).toByteArray()
}

data class CHUserKey(
    var deviceUUID: String,
    val deviceModel: String,
    val keyIndex: String,
    val secretKey: String,
    val sesame2PublicKey: String,
    var deviceName: String?,
    var keyLevel: Int,
    var rank: Int? = null,
    val subUUID: String = "",
    val stateInfo: StateInfo = StateInfo()
)

data class StateInfo(
    val batteryPercentage: Int? = null,
    val CHSesame2Status: String? = null,
    val timestamp: Long? = null,
    val wm2State: Boolean? = null
)