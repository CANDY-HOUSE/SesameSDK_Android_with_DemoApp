package co.candyhouse.app.tabs.devices.model

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.app.tabs.devices.ssm2.*
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.open.CHResultState
import co.candyhouse.sesame.open.device.*
import co.candyhouse.sesame.server.dto.CHEmpty
import co.receiver.widget.SesameForegroundService
import co.utils.SharedPreferencesUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlin.collections.ArrayList

class CHDeviceViewModel : ViewModel(), CHWifiModule2Delegate, CHSesameSensorDelegate, CHSesameTouchProDelegate {

    private val channel = Channel<Boolean>(1)

    init {
        GlobalScope.launch(IO) {
            channel.consumeAsFlow().debounce(300).collect {
                //                        L.d("hcia", "[widget 更新] isLive:" + SesameForegroundService.isLive)
                CHDeviceManager.getCandyDevices {
                    var countWidget = 0 // 紀錄 用戶開啟 Wedget 的個數
                    it.onSuccess {
                        it.data.forEach {
                            when (it) {
                                is CHSesameLock -> {
                                    if (it.getIsWidget()) {
                                        countWidget++ //有打開 widget 我就＋1
                                    }
                                }
                            }
                        }
                        if (countWidget == 0) { // 如果 沒有widget 功能。關掉 ForegroundService
                            if (SesameForegroundService.isLive) {
                                MainActivity.activity!!.stopService(Intent(MainActivity.activity!!, SesameForegroundService::class.java))
                            }
                        } else {
                            ContextCompat.startForegroundService(MainActivity.activity!!.applicationContext, Intent(MainActivity.activity!!, SesameForegroundService::class.java))
                        }
                    }
                }

            }
        }
    }

    var targetShareLevel: Int = 0
    var guestKeyId: String? = null
    var userKeys: ArrayList<String>? = null
    val myChDevices = MutableStateFlow(ArrayList<CHDevices>())
    var neeReflesh = MutableLiveData<Boolean>()
    val ssmLockLiveData = MutableLiveData<CHDevices>()
    var ssmosLockDelegates: MutableMap<CHDevices, CHDeviceStatusDelegate> = mutableMapOf()


    fun refleshDevices() {

    }


    fun updateDevices() {

//        L.d("hcia", "👘 同步本地 updateDevices:")
        CHDeviceManager.getCandyDevices {

            it.onSuccess {
                viewModelScope.launch { //                    L.d("hcia", "👘  viewModelScope:")
                    myChDevices.value.apply { //                        L.d("hcia", "👘 刷新 clear:")
                        clear()

                        myChDevices.value.addAll(it.data) //                        L.d("hcia", "👘 刷新 排序:")
                        myChDevices.value.sortWith(compareBy({ -1 * it.getRank() }, { it.uiPriority() }, { it.getNickname() }))
//                        myChDevices.value.sortWith(compareBy({ it.uiPriority() }, { it.getNickname() }))
                        //                        L.d("hcia", "👘 刷新 去權 :")
                        it.data.forEach { device ->
                            val checkyou = userKeys?.contains(device.deviceId.toString())
//                            L.d("hcia", "device.deviceId:" + device.deviceId +"  checkyou:"+checkyou)

//                            L.d("hcia", "checkyou:" + checkyou)

                            if (checkyou == false) {
                                device.dropKey { }
//                                L.d("hcia", "刷新 !!!!checkyou:" + checkyou)
                                myChDevices.value.remove(device)
                            }
                        }
                        userKeys = null

                        viewModelScope.launch { //                            L.d("hcia", "通知ＵＩ刷新 !!!!neeReflesh:" + neeReflesh)
                            neeReflesh.postValue(false)
                        }
                    }.apply {
                        forEach { device ->
                            device.delegate = this@CHDeviceViewModel
                            backgroundAutoConnect(device)
                        }
                    }
                }
            }
        }
    }


    fun updateWidgets() {
        GlobalScope.launch(IO) { //            L.d("hcia", "channel -->:")
            channel.send(true)
        }
    }

    fun dropDevice(result: CHResult<CHEmpty>) {
        val targetDevice: CHDevices = ssmLockLiveData.value!!
        targetDevice.dropKey {
            it.onSuccess {
                updateDevices()
                SharedPreferencesUtils.preferences.edit().remove(targetDevice?.deviceId.toString()).apply()
                viewModelScope.launch {
                    result.invoke(Result.success(it))
                }

            }
            it.onFailure {
                viewModelScope.launch {
                    result.invoke(Result.failure(it))
                }

            }
        }
    }

    override fun onAPSettingChanged(device: CHWifiModule2, settings: CHWifiModule2MechSettings) {
        viewModelScope.launch {
            (ssmosLockDelegates[device] as? CHWifiModule2Delegate)?.onAPSettingChanged(device, settings)
        }
    }

//    override fun onNetWorkStatusChanged(device: CHWifiModule2, settings: CHWifiModule2NetWorkStatus) { //        L.d("hcia", "總代理 onNetWorkStatusChanged settings:" + settings)
//        viewModelScope.launch {
//            (ssmosLockDelegates[device] as? CHWifiModule2Delegate)?.onNetWorkStatusChanged(device, settings)
//        }
//    }

    override fun onSSM2KeysChanged(device: CHWifiModule2, ssm2keys: Map<String, String>) {
        viewModelScope.launch {
            (ssmosLockDelegates[device] as? CHWifiModule2Delegate)?.onSSM2KeysChanged(device, ssm2keys)
        }
    }


    override fun onOTAProgress(device: CHWifiModule2, percent: Byte) {
        viewModelScope.launch {
            (ssmosLockDelegates[device] as? CHWifiModule2Delegate)?.onOTAProgress(device, percent)
        }
    }

    override fun onScanWifiSID(device: CHWifiModule2, ssid: String, rssi: Short) {
        viewModelScope.launch {
            (ssmosLockDelegates[device] as? CHWifiModule2Delegate)?.onScanWifiSID(device, ssid, rssi)
        }
    }

    override fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {

        viewModelScope.launch {
            (ssmosLockDelegates[device] as? CHWifiModule2Delegate)?.onBleDeviceStatusChanged(device, status, shadowStatus)
        }
        viewModelScope.launch {
            ssmosLockDelegates.get(device)?.onBleDeviceStatusChanged(device, status, shadowStatus)
        }
        backgroundAutoConnect(device)
        updateWidgets()
    }

    private fun backgroundAutoConnect(device: CHDevices) {// 根據不同設備判斷要不要自動斷線連線
        if (device.deviceStatus == CHDeviceStatus.ReceivedAdV) {
            if (device !is CHSesameConnector && device !is CHWifiModule2) {
                device.connect { } //自動重連
            }
        }
    }

    override fun onMechStatus(device: CHDevices) {
        viewModelScope.launch {
            (ssmosLockDelegates[device])?.onMechStatus(device)
            (ssmosLockDelegates[device] as? CHSesameTouchProDelegate)?.onMechStatus(device)
        }

    }


    override fun onFingerPrintReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {
        viewModelScope.launch {
            (ssmosLockDelegates[device] as? CHSesameTouchProDelegate)?.onFingerPrintReceive(device, ID, name, type)
        }
    }

    override fun onFingerPrintChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {
        viewModelScope.launch {
            (ssmosLockDelegates[device] as? CHSesameTouchProDelegate)?.onFingerPrintChanged(device, ID, name, type)
        }
    }

    override fun onFingerPrintReceiveEnd(device: CHSesameConnector) {
        viewModelScope.launch {
            (ssmosLockDelegates[device] as? CHSesameTouchProDelegate)?.onFingerPrintReceiveEnd(device)
        }
    }

    override fun onFingerPrintReceiveStart(device: CHSesameConnector) {
        viewModelScope.launch {
            (ssmosLockDelegates[device] as? CHSesameTouchProDelegate)?.onFingerPrintReceiveStart(device)
        }
    }

    override fun onCardReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {
        viewModelScope.launch {
            (ssmosLockDelegates[device] as? CHSesameTouchProDelegate)?.onCardReceive(device, ID, name, type)
        }
    }

    override fun onCardReceiveEnd(device: CHSesameConnector) {
        viewModelScope.launch {
            (ssmosLockDelegates[device] as? CHSesameTouchProDelegate)?.onCardReceiveEnd(device)
        }
    }

    override fun onCardChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {
        viewModelScope.launch {
            (ssmosLockDelegates[device] as? CHSesameTouchProDelegate)?.onCardChanged(device, ID, name, type)
        }
    }

    override fun onCardReceiveStart(device: CHSesameConnector) {
        viewModelScope.launch {
            (ssmosLockDelegates[device] as? CHSesameTouchProDelegate)?.onCardReceiveStart(device)
        }
    }

    override fun onKeyBoardReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {
        viewModelScope.launch {
            (ssmosLockDelegates[device] as? CHSesameTouchProDelegate)?.onKeyBoardReceive(device, ID, name, type)
        }
    }

    override fun onKeyBoardReceiveEnd(device: CHSesameConnector) {
        viewModelScope.launch {
            (ssmosLockDelegates[device] as? CHSesameTouchProDelegate)?.onKeyBoardReceiveEnd(device)
        }
    }

    override fun onKeyBoardChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {
        viewModelScope.launch {
            (ssmosLockDelegates[device] as? CHSesameTouchProDelegate)?.onKeyBoardChanged(device, ID, name, type)
        }
    }

    override fun onKeyBoardReceiveStart(device: CHSesameConnector) {
        viewModelScope.launch {
            (ssmosLockDelegates[device] as? CHSesameTouchProDelegate)?.onKeyBoardReceiveStart(device)
        }
    }

    override fun onSSM2KeysChanged(device: CHSesameConnector, ssm2keys: Map<String, ByteArray>) {
        viewModelScope.launch {
//            L.d("hcia", "ssmosLockDelegates.get(device):" + ssmosLockDelegates.get(device as CHSesameLocker))
            (ssmosLockDelegates[device] as? CHSesameSensorDelegate)?.onSSM2KeysChanged(device, ssm2keys)
            (ssmosLockDelegates[device] as? CHSesameTouchProDelegate)?.onSSM2KeysChanged(device, ssm2keys)
        }
    }


    fun resetDevice(result: CHResult<CHEmpty>) { //        L.d("hcia", "targetModel:" + targetModel)
        val targetDevice: CHDevices = ssmLockLiveData.value!!
        targetDevice.reset {
            it.onSuccess {
                updateDevices()
                viewModelScope.launch {
                    result.invoke(Result.success(it))
                }
            }


            it.onFailure {
                viewModelScope.launch {
                    result.invoke(Result.failure(it))
                }
            }
        }

    }


}