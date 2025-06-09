package co.candyhouse.sesame.open

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.CHadv
import co.candyhouse.sesame.ble.os2.CHError
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentHashMap


enum class CHBleStatus {
    opened, closed,
}

enum class CHScanStatus(val value: CHBleStatus) {
    Enable(CHBleStatus.opened), Disable(CHBleStatus.opened), BleClose(CHBleStatus.closed), Error(CHBleStatus.opened),
}

internal enum class CHDevicesServices(val value: ParcelUuid) {
    CandyHouseService(ParcelUuid(UUID.fromString("0000FD81-0000-1000-8000-00805f9b34fb"))),
}

interface CHBleManagerDelegate {
    fun didDiscoverUnRegisteredCHDevices(devices: List<CHDevices>) {}
}

interface CHBleStatusDelegate {
    fun didScanChange(ss: CHScanStatus) {}
}

@SuppressLint("MissingPermission") object CHBleManager {
    internal lateinit var appContext: Context

    init {
        CoroutineScope(IO).launch {
            while (true) {
                delay(1 * 1000L)
                delegate?.didDiscoverUnRegisteredCHDevices(chDeviceMap.map { it.value }.filter { !it.isRegistered })
            }
        }
    }


    operator fun invoke(appContext: Context) {
        CHBleManager.appContext = appContext //        L.d("hcia", "設定好！！ appContext:" + appContext)
        try {
            bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter
        } catch (e: Exception) {// 為了google 自動審查虛擬機器報錯
            L.d("hcia", "no support ble")
            return
        }
        mScanning = if (bluetoothAdapter.isEnabled) CHScanStatus.Disable else CHScanStatus.BleClose
        appContext.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                try {
                    val action = intent.action
                    if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                        when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                            BluetoothAdapter.STATE_OFF -> { //                            L.d("hcia", "🖲 藍牙 STATE_OFF:" + BluetoothAdapter.STATE_OFF)
                                disConnectAll {}
                                connectR.clear()
                                mScanning = CHScanStatus.BleClose

                                if (bluetoothAdapter.isEnabled) {
                                    bluetoothAdapter.bluetoothLeScanner?.stopScan(bleScanner)
                                }

                            }
                            BluetoothAdapter.STATE_ON -> {
                                mScanning = CHScanStatus.Disable //                            L.d("hcia", "🖲 藍牙 STATE_ON:" + BluetoothAdapter.STATE_ON)
                                enableScan {}
                            }
                        }
                    }
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))


    } //end invoke

    var statusDelegate: CHBleStatusDelegate? = null
    var delegate: CHBleManagerDelegate? = null

    lateinit var bluetoothManager: BluetoothManager

    lateinit var bluetoothAdapter: BluetoothAdapter
    internal val connectR: MutableList<String> = ArrayList()
    internal var chDeviceMap: MutableMap<String?, CHDevices> = ConcurrentHashMap()

    var mScanning: CHScanStatus = CHScanStatus.BleClose
        set(value) {
            field = value //            L.d("hcia", "🌏 藍芽狀態  " + field + " -> " + value)
            statusDelegate?.didScanChange(value)
        }
    private val bleScanner = object : ScanCallback() {

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            if (errorCode == SCAN_FAILED_ALREADY_STARTED) { //                L.d("hcia", "已經打開過了:" + errorCode)
            } else {
                L.d("hcia", "掃描錯誤 onScanFailed  errorCode:" + errorCode)
                mScanning = CHScanStatus.Error
                /// Unable to register scanner, max client reached:32
//                BluetoothAdapter.getDefaultAdapter().disable()
            }
        }

        override fun onScanResult(callbackType: Int, scanResult: ScanResult) {

//            if (BuildConfig.DEBUG) {
////                if (scanResult.rssi > -45) {
////                    L.d("hcia", "scanResult:" + scanResult.rssi + " " + scanResult.device)
////                    return
////                }
//                val test1 = "F5:4C:9E:33:C8:37"
//                if (scanResult.device?.address != test1) {
//                    return
//                }
//            }

            if (mScanning == CHScanStatus.BleClose) { //                L.d("hcia", "關掉了怎麼還有廣播 scanResult:" + scanResult?.rssi + " " + scanResult?.device)
                return
            }
            try {
                CHadv(scanResult).apply {
//                  L.d("hcia", "this.productModel:" + this.productModel)
                    this.productModel?.let {
                        chDeviceMap.getOrPut(this.deviceID.toString()) { it.deviceFactory() }.let { device ->
                            device.productModel = this.productModel!!
                            (device as? CHDeviceUtil)?.advertisement = this
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                L.d("hcia", "藍芽協議不符合 e:" + e)
            }
        }
    }

    fun getConnectRSize(): Int {
        return connectR.size
    }

    fun disableScan(result: CHResult<CHEmpty>) { //        L.d("hcia", "sdk 解除掃描:" + bluetoothAdapter)

        if (::bluetoothManager.isInitialized)return
        if (bluetoothAdapter.isEnabled) {
            bluetoothAdapter.bluetoothLeScanner.stopScan(bleScanner)
            mScanning = CHScanStatus.Disable
        } else {
            mScanning = CHScanStatus.BleClose
        }
        result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))

    }

    fun enableScan(openble: Boolean = false, result: CHResult<CHEmpty>) {
        // 检查权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                result.invoke(Result.failure(CHError.BleUnauth.value))
                return
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                result.invoke(Result.failure(CHError.BleUnauth.value))
                return
            }
            if (appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                result.invoke(Result.failure(CHError.BleUnauth.value))
                return
            }
        }

        // 检查 BluetoothAdapter
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            result.invoke(Result.failure(CHError.BleUnauth.value))
            return
        }

        // 开启蓝牙
        if (openble) {
            bluetoothAdapter.enable()
        }

        // 检查蓝牙是否开启
        if (!bluetoothAdapter.isEnabled) {
            result.invoke(Result.failure(CHError.BlePoweroff.value))
            return
        }

        // 检查扫描状态
        if (mScanning == CHScanStatus.Enable) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            return
        }

        // 停止之前的扫描
        bluetoothAdapter.bluetoothLeScanner?.stopScan(bleScanner)
        mScanning = CHScanStatus.Enable

        // 开始新的扫描
        val scanFilter = ScanFilter.Builder().setServiceUuid(CHDevicesServices.CandyHouseService.value).build()
        val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        bluetoothAdapter.bluetoothLeScanner?.startScan(mutableListOf(scanFilter), scanSettings, bleScanner)

        result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
    }



    fun disConnectAll(result: CHResult<CHEmpty>) {
        chDeviceMap.forEach {
            try {
                it.value.disconnect { }
            }catch (e:Exception){
                e.printStackTrace()
            }

        }
        result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
    }

}
