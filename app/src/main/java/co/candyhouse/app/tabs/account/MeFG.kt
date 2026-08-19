package co.candyhouse.app.tabs.account

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import co.candyhouse.app.R
import co.candyhouse.app.candyHouseApplication
import co.candyhouse.app.databinding.FgMeBinding
import co.candyhouse.app.ext.CHDeviceWrapperManager
import co.candyhouse.app.ext.aws.AWSLoginState
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.app.ext.webview.BaseNativeWebViewFragment
import co.candyhouse.app.ext.webview.manager.WebViewPoolManager
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.tabs.devices.model.CHLoginViewModel
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.devices.base.CHSesameLock
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.SharedPreferencesUtils
import co.receiver.widget.AutoUnlockForegroundService
import co.receiver.widget.AutoUnlockGeofenceManager
import co.receiver.widget.SesameWidgetNotificationManager
import co.utils.GuestUploadFlag
import co.utils.UserUtils
import co.utils.safeNavigate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 「我的」页：纯 H5 承载（me-homepage）。
 * 页面 UI（含登录态、版本号、登出、删除账号）全由 H5 渲染，native 通过 bridge 提供能力。
 */
class MeFG : BaseNativeWebViewFragment<FgMeBinding>() {

    private val tag = "MeFG"
    override val webViewName = "me-homepage"
    override fun getViewBinder() = FgMeBinding.inflate(layoutInflater)
    override fun getWebViewContainer() = bind.meWebviewContainer
    override fun getLoadingView() = bind.meLoadingProgress
    override fun getSwipeRefreshLayout() = bind.meRefresh

    private val loginViewModel: CHLoginViewModel by activityViewModels()
    private val deviceViewModel: CHDeviceViewModel by activityViewModels()

    override fun <T : View> observeViewModelData(view: T) {
        viewLifecycleOwner.lifecycleScope.launch {
            loginViewModel.gUserState.collect { loginState ->
                if (loginState == AWSLoginState.SIGNED_IN) {
                    loadSignedInUserInfo()
                }
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

    /** 打开 H5 登录页（url 由 H5 给出，native 负责跳转） */
    override fun getOnRequestLogin(): (url: String?) -> Unit = { loginUrl ->
        loginUrl?.let { url ->
            safeNavigate(
                actionId = R.id.action_to_webViewFragment,
                Bundle().apply {
                    putString("scene", "login")
                    putString("url", url)
                }
            )
        }
    }

    /** 登出成功（native 已执行 signOut）后的本地清理 */
    override fun getOnSignOutSucceeded(): () -> Unit = {
        loginViewModel.gUserState.value = AWSLoginState.SIGNED_OUT
        completeLogout()
    }

    @SuppressLint("ImplicitSamInstance")
    private fun completeLogout() {
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
        AutoUnlockGeofenceManager.clear(CHDeviceManager.app)
        SesameWidgetNotificationManager.cancelAll(
            CHDeviceManager.app,
            CHDeviceManager.listDevices
        )
        if (AutoUnlockForegroundService.isLive) {
            CHDeviceManager.app.stopService(Intent(CHDeviceManager.app, AutoUnlockForegroundService::class.java))
        }
        CHDeviceWrapperManager.clear()
        GuestUploadFlag.clear()
        WebViewPoolManager.clearWebView("contacts")
        // 登出：清掉旧 envId(history tag)，并以登出态重新订阅刷新
        SharedPreferencesUtils.environmentId = null
        requireActivity().candyHouseApplication.subscriptionManager.checkAndSubscribeToTopics()
        reloadRefresh()
    }

    private fun loadSignedInUserInfo() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            loadUserName()
            UserUtils.loadUserUserId()
        }
    }

    private suspend fun loadUserName() {
        runCatching {
            val name = AWSStatus.getUserAttributes()["name"]
            L.d(tag, "name=$name")
            SharedPreferencesUtils.name = name
        }.onFailure { e ->
            L.e(tag, "${e.message}")
        }
    }
}
