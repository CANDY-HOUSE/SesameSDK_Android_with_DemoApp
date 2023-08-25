package co.candyhouse.app.tabs

import android.app.PendingIntent
import android.content.*
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.preference.PreferenceManager
import co.candyhouse.app.NfcHandler
import co.candyhouse.app.base.BaseActivity
import co.candyhouse.app.base.NfcSetting
import co.candyhouse.app.tabs.devices.ssm2.getNFC
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.device.*
//import co.receiver.widget.LocationUpdatesService
import co.receiver.widget.SesameForegroundService
import co.utils.L
import co.utils.SharedPreferencesUtils
import co.utils.toHexString

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : BaseActivity(), OnSharedPreferenceChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = this
        onNewIntent(intent)
       // NfcHandler.ishasNfc(this)
        deviceViewModel.updateWidgets()



    }

    override fun onNewIntent(intent: Intent?) {//系統方法 有系統的 Intent 進來 ,預期處理 ＮＦＣ操作
        super.onNewIntent(intent)

      L.d("hcia", "intent:" + intent)
        if (intent?.action == "android.intent.action.MAIN") {
            return
        }

        // nfc狀態有三種 1. 沒格式化過 2.格式化過 有資料 3 格式化沒資料
        if (intent?.action == NfcAdapter.ACTION_TAG_DISCOVERED) {// 3 格式化沒資料 ?? 不確定了。有點忘了 還是ＡＰＰ在前景時近入？？tse
            val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)!!
            val nfcHexID = tag.id.toHexString()
            supportFragmentManager.fragments.firstOrNull()?.childFragmentManager?.fragments?.forEach {
                if (it is NfcSetting) {//查找當前頁面fragment 如果是設定頁面。調用設定ＮＦＣ方法 onNfcId（）
                    (it as NfcSetting).onNfcId(nfcHexID)//tse 寫的拉基  onNfcId 兩次
                }
            }
        }
        if (intent?.action == NfcAdapter.ACTION_NDEF_DISCOVERED) {// 2.格式化過 有資料??  還是ＡＰＰ在背景時近入？？tse

            val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)!!
            val nfcHexID = tag.id.toHexString()
//            L.d("hcia", "NFCid:" + tag.id.toHexString())//048e3032e26c80
            CHDeviceManager.getCandyDevices {//查出設備
                it.onSuccess {
                    it.data.filter { (it.getNFC() == nfcHexID) }.forEach { device ->//根據設備紀錄的 SharedPreferencesUtils 看看有沒有對應的nfcID
//                        L.d("hcia", "device:" + device)
                        when (device) {//check 砍掉這個 when (device)
                            is CHSesameLock -> {
                                if (device.deviceShadowStatus == null) {//如果此設備沒有被wm2 連上？
                                    CHBleManager.enableScan(true) { } // 沒有聯網就開藍芽。
                                }
                                device.connect { }// 下連接指令 可能成功？可能失敗  ？？？？ APP近來的其他生命週期有設定自動重連，理論上這行也可以不需要，但是如果有可能會連快一點？看能不能刪掉
                                when (device) {
                                    is CHSesameBot -> {
                                        var checkBLELock = true
                                        CoroutineScope(IO).launch {
                                            for (index in 0 until 5) {
//                                                L.d("hcia", "checkBLELock:" + checkBLELock + " index:" + index)
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
                                                    device.toggle {
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
                                                    device.toggle {
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
                                                        if (device.deviceStatus.value == CHDeviceLoginStatus.Login) {
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
                                                        if (device.deviceStatus.value == CHDeviceLoginStatus.Login) {
                                                            device.unlock {}
                                                            return@launch
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }//end when (device)
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

        } else {//沒格式化或是沒有寫入資料。android 背景讀取會有問題
            NfcHandler.nfcCheckInetent(intent)//格式化 nfc tag ，並且要寫入資料
        }

    }

    override fun onStart() {
        super.onStart()
//        L.d("hcia", "onStart:")
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this)

    }

    override fun onResume() {
        super.onResume()
//        L.d("hcia", "onResume:")
        CHBleManager.enableScan {}
        val nfcPendingIntent = PendingIntent.getActivity(this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        NfcAdapter.getDefaultAdapter(this)?.enableForegroundDispatch(this, nfcPendingIntent, null, null)//註冊 ＮＦＣ 系統通知
    }

    override fun onPause() {
        super.onPause()
//        L.d("hcia", "onPause:")
        NfcAdapter.getDefaultAdapter(this)?.disableForegroundDispatch(this) // 取消 nfc 系統通知
        if (!SesameForegroundService.isLive) {
            CHBleManager.disableScan {}
        }
    }


    override fun onStop() {
        super.onStop()
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this)
    }

    companion object {
        var activity: MainActivity? = null
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
//        L.d("hcia", "onSharedPreferenceChanged:" + key)
        if (key == "isNeedFreshFriend") {
            if (SharedPreferencesUtils.isNeedFreshFriend) {
                userViewModel.syncFriendsFromServer()
            }
        }
        if (key == "isNeedFreshDevice") {
            if (SharedPreferencesUtils.isNeedFreshDevice) {
                deviceViewModel.refleshDevices()
            }
        }
    }
}

