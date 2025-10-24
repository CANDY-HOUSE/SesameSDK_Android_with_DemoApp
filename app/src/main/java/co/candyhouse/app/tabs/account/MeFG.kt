package co.candyhouse.app.tabs.account

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.R
import co.candyhouse.app.databinding.FgMeBinding
import co.candyhouse.app.ext.CHDeviceWrapperManager
import co.candyhouse.app.tabs.HomeFragment
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.tabs.devices.model.CHLoginViewModel
import co.candyhouse.app.tabs.friend.ContactsWebViewManager
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.device.CHSesameLock
import co.candyhouse.sesame.utils.L
import co.receiver.widget.SesameForegroundService
import co.utils.SharedPreferencesUtils
import co.utils.UserUtils
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.objects.AlertAction
import co.utils.safeNavigate
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.UserState
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MeFG : HomeFragment<FgMeBinding>() {
    override fun getViewBinder() = FgMeBinding.inflate(layoutInflater)

    private val loginViewModel: CHLoginViewModel by activityViewModels()
    private val deviceViewModel: CHDeviceViewModel by activityViewModels()
    private var shopHomeUrl: String? = null

    override fun onPause() {
        super.onPause()
        activity?.window?.statusBarColor = ContextCompat.getColor(requireContext(), R.color.gray0)
    }

    override fun onResume() {
        super.onResume()
        bind.sysNotifyMsg.apply {
            text = if (isNotifyEnable()) getString(R.string.android_notifica_permis_on) else getString(R.string.android_notifica_permis_off)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun setupUI() {
        activity?.window?.statusBarColor = Color.WHITE
        applyRemoteConfig()
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

    override fun setupListeners() {
        bind.logoutZone.setOnClickListener {
            // 登出
            showLogoutConfirmation(0)
        }
        bind.delAccount.setOnClickListener {
            //删除
            showLogoutConfirmation(1)
        }
        bind.nameZone.setOnClickListener {
            // 名称编辑逻辑
            handleNameEdit()
        }
        bind.deviceNotification.setOnClickListener {
            // 通知
            safeNavigate(R.id.action_to_webViewFragment, Bundle().apply {
                putString("scene", "device-notify")
                putString("pushToken", SharedPreferencesUtils.deviceToken)
            })
        }
        bind.checkSysNotifyMsg.setOnClickListener {
            openSettingNotify()
        }
        bind.shop.setOnClickListener {
            safeNavigate(R.id.action_to_webViewFragment, Bundle().apply {
                putString("scene", CHDeviceManager.SHOP_FLAG)
                putString("title", getString(R.string.shop))
                putString("url", shopHomeUrl)
                putString("where", CHDeviceManager.SHOP_FLAG)
            })
        }
    }

    private fun applyRemoteConfig() {
        val showShop = Firebase.remoteConfig.getBoolean("show_shop_item")
        shopHomeUrl = Firebase.remoteConfig.getString("shop_home_url")
        L.d("RemoteConfig", "showShop is $showShop ,shopHomeUrl is $shopHomeUrl")
        bind.shop.isVisible = showShop
    }

    override fun <T : View> observeViewModelData(view: T) {
        viewLifecycleOwner.lifecycleScope.launch {
            loginViewModel.gUserState.collect { loginState ->
                updateUIForLoginState(loginState)
            }
        }
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
                ) { action ->
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
        ContactsWebViewManager.clear()
    }

    private fun handleNameEdit() {
        if (loginViewModel.gUserState.value == UserState.SIGNED_IN) {
            safeNavigate(R.id.action_to_webViewFragment, Bundle().apply {
                putString("scene", "me")
            })
        } else {
            safeNavigate(R.id.action_register_to_LoginMailFG)
        }
    }

    private fun updateUIForLoginState(loginState: UserState) {
        bind.mail.text =
            if (loginState == UserState.SIGNED_IN) AWSMobileClient.getInstance().username else getString(
                R.string.email
            )
        bind.loginStateTxt.text = loginState.name
        bind.qrcodeZone.visibility =
            if (loginState == UserState.SIGNED_IN) View.VISIBLE else View.GONE

        if (loginState == UserState.SIGNED_IN) {
            handleSignedInState()
        } else {
            handleSignedOutState()
        }
    }

    private fun handleSignedInState() {
        bind.userName.text = SharedPreferencesUtils.nickname ?: "loading..."
        bind.logoutZone.visibility = View.VISIBLE
        bind.delAccount.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            loadUserNickname()
            UserUtils.loadUserUserId()
        }
    }

    private fun handleSignedOutState() {
        bind.userName.text = getString(R.string.login)
        bind.logoutZone.visibility = View.GONE
        bind.delAccount.visibility = View.GONE
    }

    private suspend fun loadUserNickname() {
        runCatching {
            val mailName = bind.mail.text.split("@").first()
            SharedPreferencesUtils.nickname = AWSMobileClient.getInstance().getUserAttributes()["nickname"] ?: mailName
            //L.d("hcia", "設定名字:" + SharedPreferencesUtils.nickname)
        }.onFailure {
            withContext(Dispatchers.Main) {
                bind.userName.text = SharedPreferencesUtils.nickname
            }
        }.onSuccess {
            withContext(Dispatchers.Main) {
                bind.userName.text = SharedPreferencesUtils.nickname
            }
        }
    }

    private fun openSettingNotify() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.apply {
                val intent = Intent()
                intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, this.packageName)
                startActivity(intent)
            }
        }
    }

    private fun isNotifyEnable(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val notificationManager = getSystemService(
                requireContext(),
                NotificationManager::class.java
            ) as NotificationManager
            return notificationManager.areNotificationsEnabled()
        } else {
            return true
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