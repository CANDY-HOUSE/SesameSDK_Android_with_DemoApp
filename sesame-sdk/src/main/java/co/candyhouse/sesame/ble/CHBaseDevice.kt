package co.candyhouse.sesame.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.Keep
import co.candyhouse.sesame.ble.os2.CHError
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHBleManager.appContext
import co.candyhouse.sesame.open.CHBleManager.bluetoothAdapter
import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.open.CHResultState
import co.candyhouse.sesame.open.CHScanStatus
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.open.device.CHSesameProtocolMechStatus
import co.candyhouse.sesame.open.device.CHWifiModule2
import co.candyhouse.sesame.open.device.CHWifiModule2Delegate
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.utils.CHMulticastDelegate
import co.candyhouse.sesame.utils.L
import java.util.UUID

internal interface CHDeviceUtil {
    var advertisement: CHadv?//廣播
    var sesame2KeyData: CHDevice?//鑰匙的資料
    fun goIOT() {}//訂閱ＩＯＴ
    fun login(token: String? = null)
}
@Keep
@SuppressLint("MissingPermission") internal open class CHBaseDevice {
    lateinit var productModel: CHProductModel
    val gattRxBuffer: SesameBleReceiver = SesameBleReceiver() //[數據層][收]
    var gattTxBuffer: SesameBleTransmit? = null //[數據層][傳]
    lateinit var mSesameToken: ByteArray//第一次連線的時候從設備收亂數token準備驗證
    var mCharacteristic: BluetoothGattCharacteristic? = null //用來發送資料給
    var delegate: CHDeviceStatusDelegate? = null
    val multicastDelegate = CHMulticastDelegate<CHWifiModule2Delegate>()

    var deviceTimestamp:Long? = null
    var loginTimestamp:Long? = null
    var deviceId: UUID? = null
    var isRegistered: Boolean = true
    var rssi: Int? = 0
    var mBluetoothGatt: BluetoothGatt? = null //[gatt] 控制藍芽連線的全局物件
    var isNeedAuthFromServer: Boolean? = false
    var mechStatus: CHSesameProtocolMechStatus? = null
        set(value) {
            if (field != value) {
                field = value
                delegate?.onMechStatus(this as CHDevices)
                notifyMechStatusChanged()
            }
        }
    @Keep
    var deviceShadowStatus: CHDeviceStatus? = null
        set(value) {
            if (field != value) {
                field = value

                if (this is CHDevices) {
                    val device: CHDevices = this
                    delegate?.onBleDeviceStatusChanged(device, deviceStatus, device.deviceShadowStatus)
                    notifyBleDeviceStatusChanged()
                }
            }
        }
    var deviceStatus: CHDeviceStatus = CHDeviceStatus.NoBleSignal
        set(value) {
            if (field != value) {
                field = value

                if (this is CHDevices) {
                    val device: CHDevices = this
                    delegate?.onBleDeviceStatusChanged(device, device.deviceStatus, device.deviceShadowStatus)
                }
                notifyBleDeviceStatusChanged()
            }
        }
    var sesame2KeyData: CHDevice? = null
        set(value) {
            if (field != value) {
                field = value
                sesame2KeyData?.let {
                    deviceId = UUID.fromString(it.deviceUUID)!!
                    isNeedAuthFromServer = it.secretKey.contains("000000") //                    L.d("hcia", "isSubIOT:" + isSubIOT)
                    (this as CHDeviceUtil).goIOT()
                }
            }
        }

    fun dropKey(result: CHResult<CHEmpty>) {
        CHDB.CHSS2Model.deleteByDeviceId(deviceId.toString()) { deleteResult ->
            when {
                deleteResult.isSuccess -> {
                    val deletedCount = deleteResult.getOrNull() ?: 0
                    if (deletedCount > 0) {
                        delegate = null
                        deviceStatus = CHDeviceStatus.NoBleSignal
                        (this as CHDevices).disconnect {}
                        this.sesame2KeyData = null
                    }
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                }

                deleteResult.isFailure -> {
                    result.invoke(Result.failure(deleteResult.exceptionOrNull()!!))
                }
            }
        }
    }

    fun disconnect(result: CHResult<CHEmpty>) {
      L.d("hcia", "[say] 主動要求斷開藍芽連接 :" + " bluetoothAdapter.isEnabled: " + bluetoothAdapter.isEnabled + " mBluetoothGatt:" + mBluetoothGatt)
        // 在调用安卓的disconnect()断线之前，需要把原来enable的notify disable掉。否则会影响ESP32-C3 BLE 断线事件的触发。
        mBluetoothGatt?.let { gatt ->
            for (service in gatt.services) {
                L.d("[say]", "[onServicesDiscovered] service 01 : ${service.uuid}")
                if (service.uuid == Sesame2Chracs.uuidService01) {
                    for (charc in service.characteristics) {
                        L.d("[say]", "[onServicesDiscovered] charc: ${charc.uuid}")
                        if (charc.uuid == Sesame2Chracs.uuidChr03) {
                            gatt.setCharacteristicNotification(charc, true)
                            val descriptor = charc.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                            descriptor?.let {
                                it.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                                val check = gatt.writeDescriptor(it)
                                L.d("[say]", "[disconnect][NOTIFICATION]【disable】 $check")
                            }
                        }
                    }
                }
            }
        }

        if (bluetoothAdapter.isEnabled) {
            L.d("[say]", "[disconnect][start]")
            mBluetoothGatt?.disconnect()
        } else {
            mBluetoothGatt?.disconnect()
            (this as CHDeviceUtil).advertisement = null
            CHBleManager.connectR.remove(mBluetoothGatt?.device?.address)
        }
        result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
    }



}

internal fun <T> CHDevices.isBleAvailable(result: CHResult<T>): Boolean {
    // 检查位置权限（Android 6.0及以上需要）
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (CHBleManager.mScanning == CHScanStatus.BleClose) {
                result.invoke(Result.failure(CHError.BleUnauth.value))
                return false
            }
        }
    }
    if (CHBleManager.mScanning == CHScanStatus.BleClose) {
        result.invoke(Result.failure(CHError.BlePoweroff.value))
        return false
    }
    if (deviceStatus.value == CHDeviceLoginStatus.UnLogin) {
        result.invoke(Result.failure(CHError.SesameUnlogin.value))
        return false
    }
    return true
}

internal fun CHBaseDevice.toCHDevices(): CHDevices {
    return object : CHDevices {
        override var mechStatus: CHSesameProtocolMechStatus?
            get() = this@toCHDevices.mechStatus
            set(value) { this@toCHDevices.mechStatus = value }

        override var deviceTimestamp: Long?
            get() = this@toCHDevices.deviceTimestamp
            set(value) { this@toCHDevices.deviceTimestamp = value }

        override var loginTimestamp: Long?
            get() = this@toCHDevices.loginTimestamp
            set(value) { this@toCHDevices.loginTimestamp = value }

        override var delegate: CHDeviceStatusDelegate?
            get() = this@toCHDevices.delegate
            set(value) {
                this@toCHDevices.delegate = value
            }
        override val multicastDelegate: CHMulticastDelegate<CHWifiModule2Delegate>
            get() = this@toCHDevices.multicastDelegate

        override var deviceStatus: CHDeviceStatus
            get() = this@toCHDevices.deviceStatus
            set(value) { this@toCHDevices.deviceStatus = value }

        override var deviceShadowStatus: CHDeviceStatus?
            get() = this@toCHDevices.deviceShadowStatus
            set(value) { this@toCHDevices.deviceShadowStatus = value }

        override var rssi: Int?
            get() = this@toCHDevices.rssi
            set(value) { this@toCHDevices.rssi = value }

        override var deviceId: UUID?
            get() = this@toCHDevices.deviceId
            set(value) { this@toCHDevices.deviceId = value }

        override var isRegistered: Boolean
            get() = this@toCHDevices.isRegistered
            set(value) { this@toCHDevices.isRegistered = value }

        override var productModel: CHProductModel
            get() = this@toCHDevices.productModel
            set(value) { this@toCHDevices.productModel = value }

        override fun connect(result: CHResult<CHEmpty>) {
            // 实现 connect 方法
        }

        override fun disconnect(result: CHResult<CHEmpty>) {
            this@toCHDevices.disconnect(result)
        }

        override fun getKey(): CHDevice {
            return (this@toCHDevices as CHDeviceUtil).sesame2KeyData!!.copy(historyTag = null)
        }

        override fun dropKey(result: CHResult<CHEmpty>) {
            this@toCHDevices.dropKey(result)
        }

        override fun getVersionTag(result: CHResult<String>) {

        }

        override fun register(result: CHResult<CHEmpty>) {
            // 实现 register 方法
        }

        override fun reset(result: CHResult<CHEmpty>) {
            // 实现 reset 方法
        }

        override fun updateFirmware(onResponse: CHResult<BluetoothDevice>) {
            // 实现 updateFirmware 方法
        }

        // 其他方法的实现...
    }
}

internal fun CHBaseDevice.notifyBleDeviceStatusChanged() {
    multicastDelegate.invokeDelegates ({
        it.onBleDeviceStatusChanged(this as CHDevices, this.deviceStatus, this.deviceShadowStatus)
    })
}

internal fun CHBaseDevice.notifyMechStatusChanged() {
    multicastDelegate.invokeDelegates ({
        it.onMechStatus(this as CHDevices)
    })
}

internal fun CHBaseDevice.notifySesameKeysChanged() {
    multicastDelegate.invokeDelegates ({
        it.onSSM2KeysChanged(this as CHWifiModule2, emptyMap())
    })
}