package co.candyhouse.sesame.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.pm.PackageManager
import android.os.Build
import co.candyhouse.sesame.ble.os2.CHError
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.open.*
import co.candyhouse.sesame.open.CHBleManager.appContext
import co.candyhouse.sesame.open.CHBleManager.bluetoothAdapter
import co.candyhouse.sesame.open.device.*
import co.candyhouse.sesame.server.dto.*
import co.candyhouse.sesame.utils.*
import java.util.*

internal interface CHDeviceUtil {
    var advertisement: CHadv?//廣播
    var sesame2KeyData: CHDevice?//鑰匙的資料
    fun goIOT() {}//訂閱ＩＯＴ
    fun login(token: String? = null)
}
@SuppressLint("MissingPermission") internal open class CHBaseDevice {
    lateinit var productModel: CHProductModel
    val gattRxBuffer: SesameBleReceiver = SesameBleReceiver() //[數據層][收]
    var gattTxBuffer: SesameBleTransmit? = null //[數據層][傳]
    lateinit var mSesameToken: ByteArray//第一次連線的時候從設備收亂數token準備驗證
    var mCharacteristic: BluetoothGattCharacteristic? = null //用來發送資料給
    var delegate: CHDeviceStatusDelegate? = null

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
            }
        }
    var deviceShadowStatus: CHDeviceStatus? = null
        set(value) {
            if (field != value) {
                field = value
                delegate?.onBleDeviceStatusChanged(this as CHDevices, deviceStatus, field)
            }
        }
    var deviceStatus: CHDeviceStatus = CHDeviceStatus.NoBleSignal
        set(value) {
            if (field != value) {
                field = value
                delegate?.onBleDeviceStatusChanged(this as CHDevices, deviceStatus, deviceShadowStatus)
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
        CHDB.CHSS2Model.getDevice(deviceId.toString()) { ss2Keydata ->
            if (ss2Keydata.getOrNull() != null){
                CHDB.CHSS2Model.delete(ss2Keydata.getOrNull()!!) {
                    delegate = null
                    deviceStatus = CHDeviceStatus.NoBleSignal
                    (this as CHDevices).disconnect {}
                    this.sesame2KeyData = null
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                }
            }else{
                L.d("hcia", "ss2Keydata:" + ss2Keydata)
                //todo
            }

        }
    }
    fun disconnect(result: CHResult<CHEmpty>) {
//      L.d("hcia", "主動要求斷開藍芽連接 :" + " bluetoothAdapter.isEnabled:" + bluetoothAdapter.isEnabled)
        if (bluetoothAdapter.isEnabled) {
            mBluetoothGatt?.disconnect()
        } else {
            mBluetoothGatt?.disconnect()
            (this as CHDeviceUtil).advertisement = null
            CHBleManager.connectR.remove(mBluetoothGatt?.device?.address)
        }
        result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
    }



}

internal fun <T> CHDevices.checkBle(result: CHResult<T>): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (CHBleManager.mScanning == CHScanStatus.BleClose) {
                result.invoke(Result.failure(CHError.BleUnauth.value))
                return true
            }
        }
    }

    if (CHBleManager.mScanning == CHScanStatus.BleClose) {
        result.invoke(Result.failure(CHError.BlePoweroff.value))
        return true
    }
    if (deviceStatus.value == CHDeviceLoginStatus.UnLogin) {
        result.invoke(Result.failure(CHError.SesameUnlogin.value))
        return true
    }
    return false
}
