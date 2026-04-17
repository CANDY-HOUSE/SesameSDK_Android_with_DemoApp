package co.candyhouse.sesame.open.devices.base

import android.bluetooth.BluetoothDevice
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.os2.CHError
import co.candyhouse.sesame.ble.os2.CHSesame2Device
import co.candyhouse.sesame.ble.os2.CHSesameBikeDevice
import co.candyhouse.sesame.ble.os2.CHSesameBotDevice
import co.candyhouse.sesame.ble.os3.CHHub3Device
import co.candyhouse.sesame.ble.os3.CHSesame5Device
import co.candyhouse.sesame.ble.os3.CHSesameBike2Device
import co.candyhouse.sesame.ble.os3.CHSesameBike3Device
import co.candyhouse.sesame.ble.os3.CHSesameBiometricDeviceImpl
import co.candyhouse.sesame.ble.os3.CHSesameBot2Device
import co.candyhouse.sesame.ble.os3.CHWifiModule2Device
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.open.devices.BiometricDeviceType
import co.candyhouse.sesame.open.devices.CHWifiModule2Delegate
import co.candyhouse.sesame.open.devices.DeviceProfiles
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHMulticastDelegate
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import java.util.UUID

enum class CHProductModel {
    SS2 {
        override fun productType() = 0 // 設備藍芽廣播
        override fun deviceModel() = "sesame_2" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame 3"
        override fun deviceFactory() = CHSesame2Device()
    },
    WM2 {
        override fun productType() = 1 // 設備藍芽廣播
        override fun deviceModel() = "wm_2"// <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "WiFi Module 2"
        override fun deviceFactory() = CHWifiModule2Device()
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
    SS4 {
        override fun productType() = 4 // 設備藍芽廣播
        override fun deviceModel() = "sesame_4" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame 4"
        override fun deviceFactory() = CHSesame2Device()
    },
    SS5 {
        override fun productType() = 5
        override fun deviceModel() = "sesame_5" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame 5"
        override fun deviceFactory() = CHSesame5Device()
    },
    BiKeLock2 {
        override fun productType() = 6
        override fun deviceModel() = "bike_2" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Bike 2"
        override fun deviceFactory() = CHSesameBike2Device()
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
        override fun deviceFactory() = CHSesameBiometricDeviceImpl(BiometricDeviceType.OPEN_SENSOR, setOf())
    },
    SSMTouchPro {
        override fun productType() = 9
        override fun deviceModel() = "ssm_touch_pro" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Touch 1 Pro"
        override fun deviceFactory() =
            CHSesameBiometricDeviceImpl(BiometricDeviceType.SESAME_TOUCH_PRO, DeviceProfiles.SESAME_TOUCH_PRO)
    },
    SSMTouch {
        override fun productType() = 10
        override fun deviceModel() = "ssm_touch" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Touch 1"
        override fun deviceFactory() = CHSesameBiometricDeviceImpl(BiometricDeviceType.SESAME_TOUCH, DeviceProfiles.SESAME_TOUCH)
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
        override fun deviceFactory() = CHSesameBiometricDeviceImpl(BiometricDeviceType.REMOTE, setOf())
    },
    RemoteNano {
        override fun productType() = 15
        override fun deviceModel() = "remote_nano" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Remote Nano"
        override fun deviceFactory() = CHSesameBiometricDeviceImpl(BiometricDeviceType.REMOTE, setOf())
    },
    SS5US {
        override fun productType() = 16
        override fun deviceModel() = "sesame_5_us" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame 5 US"
        override fun deviceFactory() = CHSesame5Device()
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
        override fun deviceFactory() = CHSesameBiometricDeviceImpl(BiometricDeviceType.SESAME_FACE_PRO, DeviceProfiles.SESAME_FACE_PRO)
    },
    SSMFace {
        override fun productType() = 19
        override fun deviceModel() = "sesame_face" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Face 1"
        override fun deviceFactory() = CHSesameBiometricDeviceImpl(BiometricDeviceType.SESAME_FACE, DeviceProfiles.SESAME_FACE)
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
        override fun deviceFactory() = CHSesameBiometricDeviceImpl(BiometricDeviceType.SESAME_FACE_PRO_AI, DeviceProfiles.SESAME_FACE_PRO_AI)
    },
    SSMFaceAI {
        override fun productType() = 23
        override fun deviceModel() = "sesame_face_ai" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Face 1 AI"
        override fun deviceFactory() = CHSesameBiometricDeviceImpl(BiometricDeviceType.SESAME_FACE_AI, DeviceProfiles.SESAME_FACE_AI)
    },
    SSMOpenSensor2 {
        override fun productType() = 24
        override fun deviceModel() = "open_sensor_2" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Open Sensor 2"
        override fun deviceFactory() = CHSesameBiometricDeviceImpl(BiometricDeviceType.OPEN_SENSOR_2, setOf())
    },
    SSMTouch2 {
        override fun productType() = 25
        override fun deviceModel() = "ssm_touch_2" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Touch 2"
        override fun deviceFactory() = CHSesameBiometricDeviceImpl(BiometricDeviceType.SESAME_TOUCH, DeviceProfiles.SESAME_TOUCH)
    },
    SSMTouch2Pro {
        override fun productType() = 26
        override fun deviceModel() = "ssm_touch_2_pro" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Touch 2 Pro"
        override fun deviceFactory() =
            CHSesameBiometricDeviceImpl(BiometricDeviceType.SESAME_TOUCH_PRO, DeviceProfiles.SESAME_TOUCH_PRO)
    },
    SSMFace2 {
        override fun productType() = 27
        override fun deviceModel() = "sesame_face_2" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Face 2"
        override fun deviceFactory() = CHSesameBiometricDeviceImpl(BiometricDeviceType.SESAME_FACE, DeviceProfiles.SESAME_FACE)
    },
    SSMFace2Pro {
        override fun productType() = 28
        override fun deviceModel() = "ssm_face_2_pro" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Face 2 Pro"
        override fun deviceFactory() = CHSesameBiometricDeviceImpl(BiometricDeviceType.SESAME_FACE_PRO, DeviceProfiles.SESAME_FACE_PRO)
    },
    SSM_MIWA {
        override fun productType() = 29
        override fun deviceModel() = "sesame_miwa" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame miwa"
        override fun deviceFactory() = CHSesame5Device()
    },
    SSMFace2AI {
        override fun productType() = 30
        override fun deviceModel() = "sesame_face_2_ai" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Face 2 AI"
        override fun deviceFactory() = CHSesameBiometricDeviceImpl(BiometricDeviceType.SESAME_FACE_AI, DeviceProfiles.SESAME_FACE_AI)
    },
    SSMFace2ProAI {
        override fun productType() = 31
        override fun deviceModel() = "sesame_face_2_pro_ai" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Face 2 Pro AI"
        override fun deviceFactory() = CHSesameBiometricDeviceImpl(BiometricDeviceType.SESAME_FACE_PRO_AI, DeviceProfiles.SESAME_FACE_PRO_AI)
    },
    SS6ProSLiDingDoor {
        override fun productType() = 32
        override fun deviceModel() = "sesame_6_pro_slidingdoor" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame 6 Pro SLiDingDoor"
        override fun deviceFactory() = CHSesame5Device()
    },
    BiKeLock3 {
        override fun productType() = 33
        override fun deviceModel() = "bike_3" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Bike 3"
        override fun deviceFactory() = CHSesameBike3Device()
    },
    SesameBot3 {
        override fun productType() = 35
        override fun deviceModel() = "bot_3" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Sesame Bot 3"
        override fun deviceFactory() = CHSesameBot2Device()
    },
    Hub3_LTE {
        override fun productType() = 36
        override fun deviceModel() = "hub_3_lte" // <- 絕對不要動 ios/server/android必須一致
        override fun deviceModelName() = "Hub 3 LTE"
        override fun deviceFactory() = CHHub3Device()
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

interface CHDevices {
    /*
        ble tx power 根据BLE规范， 固件里可能的设置是-70~20dBm。
        实际应用的默认 SS5类的锁是 -4dBm, 刷卡机类的设备是 0dBm。
        APP 里该变量默认设置为21， 表示固件没有给这个设置的值。
        若该设备的 bee tx power 是 21， 可以认为是旧固件。

        以下内容来自固件， 供参考：
        /// Inquiry TX power level (in dBm) HCI:7.3.62
        #define INQ_TX_PWR_DBM_MIN    -70
        #define INQ_TX_PWR_DBM_DFT    0
        #define INQ_TX_PWR_DBM_MAX    +20
    */
    companion object {
        const val UNSET_BLE_TX_POWER_VALUE = 21
    }

    var mechStatus: CHSesameProtocolMechStatus?
    var deviceTimestamp: Long?
    var loginTimestamp: Long?
    var delegate: CHDeviceStatusDelegate?
    val multicastDelegate: CHMulticastDelegate<CHWifiModule2Delegate>
    var deviceStatus: CHDeviceStatus
    var deviceShadowStatus: CHDeviceStatus?
    var rssi: Int?

    // 为了解决门和墙密封较好的情况下， 蓝牙信号受影响， 门外的刷卡机与门内的锁经常断线的问题， 添加此参数。
    // 让用户可以自己设置一个合适的蓝牙信号强度阈值， 以保持BLE长连接。
    var bleTxPower: Byte
    var deviceId: UUID?
    var isRegistered: Boolean
    var productModel: CHProductModel
    var batteryPercentage: Int?

    fun connect(result: CHResult<CHEmpty>)
    fun disconnect(result: CHResult<CHEmpty>)
    fun getKey(): CHDevice {
        return (this as CHDeviceUtil).sesame2KeyData!!.copy(historyTag = null)
    }

    fun dropKey(result: CHResult<CHEmpty>)
    fun getVersionTag(result: CHResult<String>)
    fun register(result: CHResult<CHEmpty>)
    fun reset(result: CHResult<CHEmpty>)
    fun updateFirmware(onResponse: CHResult<BluetoothDevice>)
    fun updateFirmwareBleOnly(onResponse: CHResult<BluetoothDevice>) {}
    fun setBleTxPower(txPower: Byte, result: CHResult<CHEmpty>) {}
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
}

interface CHSesameConnector : CHDevices {
    var ssm2KeysMap: MutableMap<String, ByteArray>
    fun insertSesame(sesame: CHDevices, result: CHResult<CHEmpty>)
    fun removeSesame(tag: String, result: CHResult<CHEmpty>)
    fun setRadarSensitivity(payload: ByteArray, result: CHResult<CHEmpty>) {}
}

interface CHSesameLock : CHDevices {
    fun disableNotification(fcmToken: String, result: CHResult<Any>) {
        CHAPIClientBiz.cancelNotification(this, fcmToken) { it ->
            it.onSuccess {
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(it.data)))
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
    val isCritical: Boolean?
        get() = null

    val data: ByteArray
}

interface CHDeviceStatusDelegate {
    fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {}
    fun onMechStatus(device: CHDevices) {}
    fun onBleTxPowerReceive(device: CHDevices, txPower: Byte) {}
}

enum class CHDeviceStatus(val value: CHDeviceLoginStatus) {
    NoBleSignal(CHDeviceLoginStatus.unlogined),
    ReceivedAdV(CHDeviceLoginStatus.unlogined),
    BleConnecting(CHDeviceLoginStatus.unlogined),
    DiscoverServices(CHDeviceLoginStatus.unlogined),
    BleLogining(CHDeviceLoginStatus.unlogined),
    Registering(CHDeviceLoginStatus.unlogined),
    ReadyToRegister(CHDeviceLoginStatus.unlogined),
    WaitingGatt(CHDeviceLoginStatus.unlogined),
    WaitingForAuth(CHDeviceLoginStatus.unlogined),
    NoSettings(CHDeviceLoginStatus.logined),
    Reset(CHDeviceLoginStatus.unlogined),
    DfuMode(CHDeviceLoginStatus.unlogined),
    Busy(CHDeviceLoginStatus.unlogined),
    Locked(CHDeviceLoginStatus.logined),
    Moved(CHDeviceLoginStatus.logined),
    Unlocked(CHDeviceLoginStatus.logined),
    WaitApConnect(CHDeviceLoginStatus.logined),
    IotConnected(CHDeviceLoginStatus.logined),
    IotDisconnected(CHDeviceLoginStatus.logined)
}

enum class CHDeviceLoginStatus {
    logined, unlogined
}

class NSError(message: String, var domain: String, var code: Int) : Error(message)