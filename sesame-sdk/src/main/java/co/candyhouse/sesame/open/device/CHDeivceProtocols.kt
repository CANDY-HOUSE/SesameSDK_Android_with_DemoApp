package co.candyhouse.sesame.open.device
import android.bluetooth.BluetoothDevice
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.os2.CHError
import co.candyhouse.sesame.ble.os2.CHSesame2Device
import co.candyhouse.sesame.ble.os2.CHSesameBikeDevice
import co.candyhouse.sesame.ble.os2.CHSesameBotDevice
import co.candyhouse.sesame.ble.os3.CHHub3Device
import co.candyhouse.sesame.ble.os3.CHSesame5Device
import co.candyhouse.sesame.ble.os3.CHSesameBike2Device
import co.candyhouse.sesame.ble.os3.CHSesameBot2Device
import co.candyhouse.sesame.ble.os3.CHWifiModule2Device
import co.candyhouse.sesame.ble.os3.biometric.face.CHSesameFaceDevice
import co.candyhouse.sesame.ble.os3.biometric.facePro.CHSesameFaceProDevice
import co.candyhouse.sesame.ble.os3.biometric.touch.CHSesameTouchDevice
import co.candyhouse.sesame.ble.os3.biometric.touchPro.CHSesameTouchProDevice
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.open.CHAccountManager
import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.open.CHResultState
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.server.dto.CHGuestKey
import co.candyhouse.sesame.server.dto.CHGuestKeyCut
import co.candyhouse.sesame.server.dto.CHModifyGuestKeyRequest
import co.candyhouse.sesame.server.dto.CHRemoveGuestKeyRequest
import co.candyhouse.sesame.utils.CHMulticastDelegate
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.toHexString
import co.candyhouse.sesame.utils.toUInt24ByteArray
import com.amazonaws.mobileconnectors.apigateway.ApiClientException
import java.util.UUID

enum class MatterProductModel(val value: UByte) {
    DoorLock(0u),
    OnOffSwitch(1u),
    OnOffSensor(4u), /*https://github.com/CANDY-HOUSE/SesameOS3_esp32c3/blob/master/src/app/zap-templates/zcl/data-model/chip/matter-devices.xml#L1311 */
    None(255u);
}

enum class CHProductModel {
    WM2 {
        override fun productType() = 1 // 設備藍芽廣播
        override fun deviceModel() = "wm_2"// <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "WiFi Module 2"
        override fun deviceFactory() = CHWifiModule2Device()
    },
    SS2 {
        override fun productType() = 0 // 設備藍芽廣播
        override fun deviceModel() = "sesame_2" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame 3"
        override fun deviceFactory() = CHSesame2Device()
    },
    SS4 {
        override fun productType() = 4 // 設備藍芽廣播
        override fun deviceModel() = "sesame_4" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame 4"
        override fun deviceFactory() = CHSesame2Device()
    },

    SesameBot1 {
        override fun productType() = 2 // 設備藍芽廣播
        override fun deviceModel() = "ssmbot_1" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Bot 1"
        override fun deviceFactory() = CHSesameBotDevice()
    },

    BiKeLock {
        override fun productType() = 3
        override fun deviceModel() = "bike_1" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Bike 1"
        override fun deviceFactory() = CHSesameBikeDevice()
    },

    BiKeLock2 {
        override fun productType() = 6
        override fun deviceModel() = "bike_2" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Bike 2"
        override fun deviceFactory() = CHSesameBike2Device()
    },
    SS5 {
        override fun productType() = 5
        override fun deviceModel() = "sesame_5" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame 5"
        override fun deviceFactory() = CHSesame5Device()
    },
    SS5PRO {
        override fun productType() = 7
        override fun deviceModel() = "sesame_5_pro" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame 5 Pro"
        override fun deviceFactory() = CHSesame5Device()
    },
    SSMOpenSensor {
        override fun productType() = 8
        override fun deviceModel() = "open_sensor_1" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Open Sensor 1"
        override fun deviceFactory() = CHSesameTouchProDevice()
    },
    SSMTouchPro {
        override fun productType() = 9
        override fun deviceModel() = "ssm_touch_pro" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Touch 1 Pro"
        override fun deviceFactory() = CHSesameTouchProDevice()
    },
    SSMTouch {
        override fun productType() = 10
        override fun deviceModel() = "ssm_touch" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Touch 1"
        override fun deviceFactory() = CHSesameTouchDevice()
    },
    BLEConnector {
        override fun productType() = 11
        override fun deviceModel() = "BLE_Connector_1" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "BLE Connector 1"
        override fun deviceFactory() = CHSesame5Device()
    },
    Hub3 {
        override fun productType() = 13
        override fun deviceModel() = "hub_3" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Hub 3"
        override fun deviceFactory() = CHHub3Device()
    },
    Remote {
        override fun productType() = 14
        override fun deviceModel() = "remote" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Remote"
        override fun deviceFactory() = CHSesameTouchProDevice()
    },
    RemoteNano {
        override fun productType() = 15
        override fun deviceModel() = "remote_nano" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Remote Nano"
        override fun deviceFactory() = CHSesameTouchProDevice()
    },
    SS5US {
        override fun productType() = 16
        override fun deviceModel() = "sesame_5_us" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame 5 US"
        override fun deviceFactory() = CHSesame5Device()
        //  override fun deviceFactory() = CHSesame2Device()//  用 ss4 协议登入 ss5
    },
    SesameBot2 {
        override fun productType() = 17
        override fun deviceModel() = "bot_2" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Bot 2"
        override fun deviceFactory() = CHSesameBot2Device()
    },
    SSMFacePro {
        override fun productType() = 18
        override fun deviceModel() = "sesame_face_Pro" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Face 1 Pro"
        override fun deviceFactory() = CHSesameFaceProDevice()
    },
    SSMFace {
        override fun productType() = 19
        override fun deviceModel() = "sesame_face" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Face 1"
        override fun deviceFactory() = CHSesameFaceDevice()
    },
    SS6Pro {
        override fun productType() = 21
        override fun deviceModel() = "sesame_6_pro" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame 6 Pro"
        override fun deviceFactory() = CHSesame5Device()
    },
    SSMFaceProAI {
        override fun productType() = 22
        override fun deviceModel() = "sesame_face_pro_ai" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Face 1 Pro AI"
        override fun deviceFactory() = CHSesameFaceProDevice()
    },
    SSMFaceAI {
        override fun productType() = 23
        override fun deviceModel() = "sesame_face_ai" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Face 1 AI"
        override fun deviceFactory() = CHSesameFaceProDevice()
    };

    abstract fun productType(): Int
    abstract fun deviceModel(): String
    abstract fun deviceModelName(): String
    internal abstract fun deviceFactory(): CHDevices

    companion object {
        private val values = values()
        fun getByValue(value: Int) = values.firstOrNull { it.productType() == value }
        fun getByModel(value: String) = values.firstOrNull { it.deviceModel() == value }
    }
}

//interface CHDeviceStatusAndKeysDelegate : CHDeviceStatusDelegate, CHWifiModule2Delegate {}
interface CHDevices {

    var mechStatus: CHSesameProtocolMechStatus?
    var deviceTimestamp: Long?
    var loginTimestamp: Long?

    var delegate: CHDeviceStatusDelegate?
    val multicastDelegate: CHMulticastDelegate<CHWifiModule2Delegate>

    var deviceStatus: CHDeviceStatus
    var deviceShadowStatus: CHDeviceStatus?
    var rssi: Int?

    var deviceId: UUID?
    var isRegistered: Boolean
    var productModel: CHProductModel

    fun connect(result: CHResult<CHEmpty>)

    fun disconnect(result: CHResult<CHEmpty>)
//    fun disconnect(result: CHResult<CHEmpty>) {
//        (this as CHDeviceUtil).mDisconnect(result)
//    }

    //    fun getKey(): CHDevice
    fun getKey(): CHDevice {
        return (this as CHDeviceUtil).sesame2KeyData!!.copy(historyTag = null)
    }

    fun dropKey(result: CHResult<CHEmpty>)

    fun getVersionTag(result: CHResult<String>)
    fun register(result: CHResult<CHEmpty>)
    fun reset(result: CHResult<CHEmpty>)
    fun updateFirmware(onResponse: CHResult<BluetoothDevice>)

    fun createGuestKey(keyName: String, result: CHResult<CHDevice>) {
        CHAccountManager.generateGuestKey(CHGuestKey(getKey().deviceUUID, getKey().deviceModel, getKey().keyIndex, getKey().secretKey, getKey().sesame2PublicKey, keyName)) {
            it.onSuccess {
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(getKey().copy(secretKey = it.data, historyTag = null))))
                L.d("hcia", "生成鑰匙成功:" + it.data)
            }
            it.onFailure {
                result.invoke(Result.failure(it))
            }
        }
    }

    fun getGuestKeys(result: CHResult<Array<CHGuestKeyCut>>) {
        CHAccountManager.getGuestKeys(this) {
            it.onSuccess {
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(it.data)))
            }
            it.onFailure {
                result.invoke(Result.failure(it))
            }
        }
    }

    fun removeGuestKey(guestKeyId: String, result: CHResult<CHEmpty>) {
        val msg = System.currentTimeMillis().toUInt24ByteArray()
        val keyCheck = (AesCmac(getKey().secretKey.hexStringToByteArray(), 16).computeMac(msg)!!).sliceArray(0..3)
        CHAccountManager.removeGuestKey(CHRemoveGuestKeyRequest(deviceId.toString().toUpperCase(), guestKeyId, keyCheck.toHexString())) {
            it.onSuccess {
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(CHEmpty())))
            }
            it.onFailure {
                result.invoke(Result.failure(it))
            }
        }
    }

    fun updateGuestKey(guestKeyId: String, name: String, result: CHResult<CHEmpty>) {
        CHAccountManager.changeGuestKeyName(deviceId.toString(), CHModifyGuestKeyRequest(guestKeyId, name)) {
            it.onSuccess {
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(CHEmpty())))
                L.d("hcia", "it.data:" + it.data)
            }
            it.onFailure {
                result.invoke(Result.failure(it))
            }
        }
    }

    fun setHistoryTag(tag: ByteArray, result: CHResult<CHEmpty>) {
        if ((this as CHDeviceUtil).sesame2KeyData == null) {
            result.invoke(Result.failure(CHError.BleUnauth.value))
            return
        }
        sesame2KeyData!!.historyTag = tag.copyOf()
        CHDB.CHSS2Model.insert(sesame2KeyData!!.copy(historyTag = tag.copyOf())) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    fun getHistoryTag(): ByteArray? {
        return (this as CHDeviceUtil).sesame2KeyData?.historyTag
    }

    fun getTimeSignature(): String? {
        if (this is CHDeviceUtil) {


            try {
                var secretKey = sesame2KeyData?.secretKey ?: return null
                val keyCheck = AesCmac(secretKey.hexStringToByteArray(), 16).computeMac(System.currentTimeMillis().toUInt24ByteArray())!!.sliceArray(0..3)
                return keyCheck.toHexString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return null
    }

}

interface CHSesameConnector : CHDevices {
    var ssm2KeysMap: MutableMap<String, ByteArray>
    fun insertSesame(sesame: CHDevices, result: CHResult<CHEmpty>)
    fun removeSesame(tag: String, result: CHResult<CHEmpty>)
    fun setRadarSensitivity(payload: ByteArray, result: CHResult<CHEmpty>){}
}

interface CHSesameLock : CHDevices {

    fun isEnableNotification(fcmToken: String, result: CHResult<Boolean>) {
        CHAccountManager.isNotificationEnable(this, fcmToken) {
            it.onSuccess {
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(true)))
            }
            it.onFailure {
                if (it is ApiClientException) {
                    if (it.statusCode == 404) {
                        result.invoke(Result.success(CHResultState.CHResultStateNetworks(false)))
                        return@onFailure
                    }
                }
                result.invoke(Result.failure(it))
            }
        }
    }

    fun enableNotification(fcmToken: String, subUUID:String, result: CHResult<Any>) {
        L.d("hcia", "String:" + String)
        CHAccountManager.enableNotification(this, fcmToken, subUUID) {
            it.onSuccess {
                L.d("hcia", "it:" + it)
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(it.data)))
            }
            it.onFailure {
                L.d("hcia", "it:" + it)
            }
        }
    }

    fun disableNotification(fcmToken: String, subUUID: String = "", result: CHResult<Any>) {
        CHAccountManager.cancelNotification(this, fcmToken) {
            it.onSuccess {
                L.d("hcia", "it:" + it)
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(it.data)))
            }
            it.onFailure {
                L.d("hcia", "it:" + it)
            }
        }
    }
}

interface CHSesameProtocolMechStatus {

    val position: Short
        get() = 0
    val target: Short?
        get() = 0
    val isBatteryCritical: Boolean
        get() = false
    val isInLockRange: Boolean
        get() = false
    val isInUnlockRange: Boolean
        get() = !isInLockRange
    val isStop: Boolean?
        get() = null

    val data: ByteArray

    fun getBatteryVoltage(): Float
    fun getBatteryPrecentage(): Int {
        val voltage = getBatteryVoltage()
        /*
        *    修正电池电量显示不准的问题。(仅限Sesame5系列芯片的ADC)
        *
        *    1、 在刷卡机上，用万用表实测电源电压为6V时，芯片读到的单节电池ADC值乘以2后，为5850mV。
        *    2、 调整固件里的采样次数和GPIO的开关时机，实测对这个误差无影响。
        *    3、 用示波器实测，对比芯片ADC测量的值，可以近似认为芯片ADC读到的是电源电压除以2的最小值，但是APP显示的应该是平均值。所以需要做相应的修正。
        *    4、 其他电压也有类似现象，但是变化不是线性的。
        *    5、 多次测量后，根据实测值，修正电量显示对应的List。
        * */
        val blocks: List<Float> = listOf(5.85f, 5.82f, 5.79f, 5.76f, 5.73f, 5.70f, 5.65f, 5.60f, 5.55f, 5.50f, 5.40f, 5.20f, 5.10f, 5.0f, 4.8f, 4.6f)
        val mapping: List<Float> = listOf(100.0f, 95.0f, 90.0f, 85.0f, 80.0f, 70.0f, 60.0f, 50.0f, 40.0f, 32.0f, 21.0f, 13.0f, 10.0f, 7.0f, 3.0f, 0.0f)
        if (voltage >= blocks[0]) {
            return mapping[0].toInt()
        }
        if (voltage <= blocks[blocks.size - 1]) {
            return (mapping[mapping.size - 1]).toInt()
        }
        for (i in 0 until blocks.size - 1) {
            val upper: Float = blocks[i]
            val lower: Float = blocks[i + 1]
            if (voltage <= upper && voltage > lower) {
                val value: Float = (voltage - lower) / (upper - lower)
                val max = mapping[i]
                val min = mapping[i + 1]
                return ((max - min) * value + min).toInt()
            }
        }
        return 0
    }

}

interface CHDeviceStatusDelegate {
    fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {}
    fun onMechStatus(device: CHDevices) {}
}

enum class CHDeviceStatus(val value: CHDeviceLoginStatus) {
    NoBleSignal(CHDeviceLoginStatus.UnLogin),
    ReceivedAdV(CHDeviceLoginStatus.UnLogin),
    BleConnecting(CHDeviceLoginStatus.UnLogin),
    DiscoverServices(CHDeviceLoginStatus.UnLogin),
    BleLogining(CHDeviceLoginStatus.UnLogin),
    Registering(CHDeviceLoginStatus.UnLogin),
    ReadyToRegister(CHDeviceLoginStatus.UnLogin),
    WaitingForAuth(CHDeviceLoginStatus.UnLogin),
    NoSettings(CHDeviceLoginStatus.Login),
    Reset(CHDeviceLoginStatus.UnLogin),
    DfuMode(CHDeviceLoginStatus.UnLogin),
    Busy(CHDeviceLoginStatus.UnLogin),
    Locked(CHDeviceLoginStatus.Login),
    Moved(CHDeviceLoginStatus.Login),
    Unlocked(CHDeviceLoginStatus.Login),
    WaitApConnect(CHDeviceLoginStatus.Login),
    IotConnected(CHDeviceLoginStatus.Login),
    IotDisconnected(CHDeviceLoginStatus.Login)
}

enum class CHDeviceLoginStatus {
    Login, UnLogin,
}

class NSError(message: String, var domain: String, var code: Int) : Error(message)

