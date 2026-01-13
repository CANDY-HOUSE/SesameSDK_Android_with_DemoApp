package co.candyhouse.app.tabs

import android.Manifest
import android.app.AppOpsManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseActivity
import co.candyhouse.app.base.NfcSetting
import co.candyhouse.app.base.setPage
import co.candyhouse.app.ext.NfcHandler
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.app.ext.webview.manager.WebViewPoolManager
import co.candyhouse.app.tabs.devices.ssm2.getNFC
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHSesame2
import co.candyhouse.sesame.open.device.CHSesame5
import co.candyhouse.sesame.open.device.CHSesameBike
import co.candyhouse.sesame.open.device.CHSesameBike2
import co.candyhouse.sesame.open.device.CHSesameBot
import co.candyhouse.sesame.open.device.CHSesameBot2
import co.candyhouse.sesame.open.device.CHSesameLock
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.SharedPreferencesUtils
import co.receiver.widget.SesameForegroundService
import co.utils.AnalyticsUtil
import co.utils.UserUtils
import co.utils.toHexString
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.lang.reflect.InvocationTargetException

class MainActivity : BaseActivity(), OnSharedPreferenceChangeListener {

    private val requestCodeNFC = 100
    private var pendingOpenWebViewUrl: String? = null

    companion object {
        var activity: MainActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = this
        onNewIntent(intent)

        initNeedSharedParams()

        // 先显示本地数据
        deviceViewModel.updateDevices()

        if (AWSStatus.isInitialized()) {
            setupAWSFeatures()
        } else {
            AWSStatus.initAWSMobileClient(this) { isLoggedIn ->
                setupAWSFeatures()
            }
        }
        setAWSUserStateListener()
    }

    private fun setupAWSFeatures() {
        val userState = AWSStatus.getCachedUserState()
        loginViewModel.gUserState.value = userState
        deviceViewModel.refreshDevices()
    }

    private fun initNeedSharedParams() {
        SharedPreferencesUtils.isNeedFreshFriend = false
        SharedPreferencesUtils.isNeedFreshDevice = false
    }

    override fun onStart() {
        super.onStart()
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)
        CoroutineScope(IO).launch {
            UserUtils.loadUserUserId()
        }
    }

    override fun onResume() {
        super.onResume()
        CHBleManager.enableScan {}
        deviceViewModel.handleAppGoToForeground()
        checkNfcAdapterPermissions()
    }

    override fun onPostResume() {
        super.onPostResume()
        tryOpenPendingWebView()
    }

    override fun onPause() {
        super.onPause()
        handleOnPause()
    }

    override fun onDestroy() {
        try {
            PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
        activity = null
        if (isFinishing) {
            WebViewPoolManager.clearAll()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        intent?.let { handleNotificationIntent(it) }

        // 如果是主Activity启动的Intent，直接返回
        if (intent?.action == "android.intent.action.MAIN") return

        try {
            // nfc狀態有三種 1. 沒格式化過 2.格式化過 有資料 3 格式化沒資料
            if (intent?.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
                // 3 格式化沒資料 ?? 不確定了。有點忘了 還是ＡＰＰ在前景時近入？？tse
                val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)!!
                val nfcHexID = tag.id.toHexString()
                supportFragmentManager.fragments.firstOrNull()?.childFragmentManager?.fragments?.forEach {
                    if (it is NfcSetting) {//查找當前頁面fragment 如果是設定頁面。調用設定ＮＦＣ方法 onNfcId（）
                        (it as NfcSetting).onNfcId(nfcHexID)//tse 寫的拉基  onNfcId 兩次
                    }
                }
            }

            if (intent?.action == NfcAdapter.ACTION_NDEF_DISCOVERED) {
                // 2.格式化過 有資料??  還是ＡＰＰ在背景時近入？？tse
                val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)!!
                val nfcHexID = tag.id.toHexString()

                CHDeviceManager.getCandyDevices {//查出設備
                    it.onSuccess {
                        it.data.filter { (it.getNFC() == nfcHexID) }
                            .forEach { device ->//根據設備紀錄的 SharedPreferencesUtils 看看有沒有對應的nfcID
                                when (device) {//check 砍掉這個 when (device)
                                    is CHSesameLock -> {
                                        if (device.deviceShadowStatus == null) {//如果此設備沒有被wm2 連上？
                                            CHBleManager.enableScan(true) { } // 沒有聯網就開藍芽。
                                        }
                                        device.connect { }// 下連接指令 可能成功？可能失敗  ？？？？ APP近來的其他生命週期有設定自動重連，理論上這行也可以不需要，但是如果有可能會連快一點？看能不能刪掉

                                        dispatchOnDevices(device)
                                    }
                                }
                            }
                    }
                }

                supportFragmentManager.fragments.firstOrNull()?.childFragmentManager?.fragments?.forEach {
                    if (it is NfcSetting) {
                        (it as NfcSetting).onNfcId(nfcHexID)//tse 寫了拉基  onNfcId兩次？？
                    }
                }
            } else {
                NfcHandler.nfcCheckInetent(intent)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
            hasUsageStatsPermission(this)
        }
    }

    private fun dispatchOnDevices(device: CHSesameLock) {
        when (device) {
            is CHSesameBot -> {
                var checkBLELock = true
                CoroutineScope(IO).launch {
                    for (index in 0 until 5) {
                        if (checkBLELock) {
                            device.click {
                                it.onSuccess {
                                    checkBLELock = false
                                }
                            }
                            delay(2000)
                        }
                    }
                }
            }

            is CHSesame2 -> {//sesameOS2 ==> model--> sesame4  sesame2(客服認知sesame3)
                var checkBLELock = true// 記錄開鎖成功沒？
                CoroutineScope(IO).launch {
                    for (index in 0 until 5) {//每隔兩秒開一次開開
                        if (checkBLELock) {
                            device.toggle() {
                                it.onSuccess {
                                    checkBLELock = false
                                }
                                it.onFailure {}
                            }
                            delay(2000)
                        }
                    }
                }
            }

            is CHSesame5 -> {
                var checkBLELock = true// 記錄開鎖成功沒？
                CoroutineScope(IO).launch {
                    for (index in 0 until 5) {//每隔兩秒開一次開開
                        if (checkBLELock) {
                            device.toggle(historytag = UserUtils.getUserIdWithByte()) {
                                it.onSuccess {
                                    checkBLELock = false
                                }
                                it.onFailure {}
                            }
                            delay(2000)
                        }
                    }
                }
            }

            is CHSesameBike -> {
                device.unlock {
                    it.onFailure {
                        GlobalScope.launch {
                            repeat(8) {
                                delay(1000)
                                if (device.deviceStatus.value == CHDeviceLoginStatus.logined) {
                                    device.unlock {}
                                    return@launch
                                }
                            }
                        }
                    }
                }
            }

            is CHSesameBike2 -> {
                device.unlock {
                    it.onFailure {
                        GlobalScope.launch {
                            repeat(8) {
                                delay(1000)
                                if (device.deviceStatus.value == CHDeviceLoginStatus.logined) {
                                    device.unlock {}
                                    return@launch
                                }
                            }
                        }
                    }
                }
            }

            is CHSesameBot2 -> {
                device.click {
                    it.onFailure {
                        GlobalScope.launch {
                            repeat(8) {
                                delay(1000)
                                if (device.deviceStatus.value == CHDeviceLoginStatus.logined) {
                                    device.click {}
                                    return@launch
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkNfcAdapterPermissions() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter != null) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.NFC
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.NFC),
                    requestCodeNFC
                )
            } else {
                enableNfcForegroundDispatch(nfcAdapter)
            }
        } else {
            L.d("hcia", "NFC is not supported or disabled on this device.")
        }
    }

    private fun handleOnPause() {
        if (!isFinishing && !isDestroyed) {
            try {
                val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
                nfcAdapter?.disableForegroundDispatch(this)

                if (!SesameForegroundService.isLive) {
                    CHBleManager.disableScan {}
                }
            } catch (e: IllegalStateException) {
                L.d(
                    "hcia",
                    "IllegalStateException when disabling foreground dispatch: ${e.message}"
                )
            }
        }
    }

    private fun hasUsageStatsPermission(context: Context) {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )

        if (mode != AppOpsManager.MODE_ALLOWED) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivityForResult(intent, 306)
        }
    }

    private fun enableNfcForegroundDispatch(nfcAdapter: NfcAdapter) {
        val nfcPendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        try {
            nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, null, null)
        } catch (e: SecurityException) {
            L.d("MainActivity", "SecurityException in enableForegroundDispatch: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodeNFC) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
                if (nfcAdapter != null) {
                    enableNfcForegroundDispatch(nfcAdapter)
                }
            } else {
                L.d("MainActivity", "NFC permission denied")
            }
        }
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
        if (key == "isNeedFreshFriend") {
            if (SharedPreferencesUtils.isNeedFreshFriend) {
                WebViewPoolManager.setPendingRefresh("contacts")
                navController.navigateUp()
                findViewById<BottomNavigationView>(R.id.bottom_nav)?.setPage(1)
                SharedPreferencesUtils.isNeedFreshFriend = false
            }
        }
        if (key == "isNeedFreshDevice") {
            if (SharedPreferencesUtils.isNeedFreshDevice) {
                deviceViewModel.refreshDevices()
                SharedPreferencesUtils.isNeedFreshDevice = false
            }
        }
    }

    private fun handleNotificationIntent(intent: Intent) {
        L.d("Notification", "handleNotificationIntent called - action=${intent.action} data=${intent.data}")

        intent.data?.let { uri ->
            if (uri.scheme == "candyhouse") {
                val url = uri.getQueryParameter("url")
                val action = uri.getQueryParameter("action")

                L.d("Notification", "从 URI 提取: action=$action url=$url")

                if (action == "open_webview" && !url.isNullOrBlank()) {
                    L.e("Notification", "设置 pendingOpenWebViewUrl=$url")
                    pendingOpenWebViewUrl = url
                    intent.data = null
                    AnalyticsUtil.logButtonClick(action)
                }
            }
        }
    }

    private fun tryOpenPendingWebView() {
        val url = pendingOpenWebViewUrl ?: return

        try {
            val bundle = Bundle().apply {
                putString("url", url)
                putString("where", CHDeviceManager.NOTIFICATION_FLAG)
            }
            navController.navigate(R.id.webViewFragment, bundle)
            pendingOpenWebViewUrl = null
        } catch (_: Exception) {
        }
    }
}
