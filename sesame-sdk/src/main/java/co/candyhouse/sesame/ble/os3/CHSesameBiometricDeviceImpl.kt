package co.candyhouse.sesame.ble.os3

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.CHadv
import co.candyhouse.sesame.ble.DeviceSegmentType
import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.SSM3ResponsePayload
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.isBleAvailable
import co.candyhouse.sesame.ble.os3.base.CHSesameOS3
import co.candyhouse.sesame.ble.os3.base.SesameOS3BleCipher
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.open.devices.BiometricCapability
import co.candyhouse.sesame.open.devices.BiometricDeviceType
import co.candyhouse.sesame.open.devices.CHSesameBiometricDevice
import co.candyhouse.sesame.open.devices.base.CHDeviceStatus
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.base.CHProductModel
import co.candyhouse.sesame.open.devices.base.NSError
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHAutoInitCapabilityImpl
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHEventHandler
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.card.CHCardCapable
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.card.CHCardCapableImpl
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.connect.CHDeviceConnectCapableImpl
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.connect.CHDeviceConnectDelegate
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.face.CHFaceCapable
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.face.CHFaceCapableImpl
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.fingerPrint.CHFingerPrintCapable
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.fingerPrint.CHFingerPrintCapableImpl
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.palm.CHPalmCapable
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.palm.CHPalmCapableImpl
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.passcode.CHPassCodeCapable
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.passcode.CHPassCodeCapableImpl
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.remoteNano.CHRemoteNanoCapable
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.remoteNano.CHRemoteNanoCapableImpl
import co.candyhouse.sesame.open.devices.sesameBiometric.parseData.CHRemoteNanoTriggerSettings
import co.candyhouse.sesame.open.devices.sesameBiometric.parseData.CHSesameOpenSensorMechStatus
import co.candyhouse.sesame.open.devices.sesameBiometric.parseData.CHSesameTouchProMechStatus
import co.candyhouse.sesame.open.devices.sesameBiometric.parseData.OpenSensorData
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.CHIotManager
import co.candyhouse.sesame.server.dto.CHOS3RegisterReq
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.utils.EccKey
import co.candyhouse.sesame.utils.Event
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame.utils.base64decodeHex
import co.candyhouse.sesame.utils.divideArray
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.isInternetAvailable
import co.candyhouse.sesame.utils.noHashtoUUID
import co.candyhouse.sesame.utils.toUInt32ByteArray

/**
 * 
 *
 * @author frey on 2026/3/31
 */
@SuppressLint("MissingPermission")
internal class CHSesameBiometricDeviceImpl(
    override val deviceType: BiometricDeviceType,
    private val capabilities: Set<BiometricCapability>
) : CHSesameOS3(),
    CHSesameBiometricDevice,
    CHDeviceUtil,
    CHCardCapable by CHCardCapableImpl(),
    CHPassCodeCapable by CHPassCodeCapableImpl(),
    CHFingerPrintCapable by CHFingerPrintCapableImpl(),
    CHPalmCapable by CHPalmCapableImpl(),
    CHFaceCapable by CHFaceCapableImpl(),
    CHRemoteNanoCapable by CHRemoteNanoCapableImpl() {

    private val tag = "CHSesameBiometricDeviceImpl"

    override fun supportedCapabilities() = capabilities

    override var advertisement: CHadv? = null
        set(value) {
            field = value
            parceADV(value)
        }

    override var triggerDelaySetting: CHRemoteNanoTriggerSettings? = null
    override var ssm2KeysMap: MutableMap<String, ByteArray> = ObservableMutableMap()

    protected var isConnectedByWM2: Boolean = false

    private val eventHandlers = mutableListOf<CHEventHandler>()
    private val deviceConnectCapability = CHDeviceConnectCapableImpl()

    init {
        deviceConnectCapability.setSupport(this)

        listOf(
            this as CHCardCapable,
            this as CHPassCodeCapable,
            this as CHFingerPrintCapable,
            this as CHPalmCapable,
            this as CHFaceCapable,
            this as CHRemoteNanoCapable
        ).forEach {
            (it as? CHAutoInitCapabilityImpl)?.setupSupport(this)
        }
    }

    companion object {
        private val IOT_BATTERY_OPENSENSOR_MODELS = setOf(
            CHProductModel.SSMOpenSensor,
            CHProductModel.SSMOpenSensor2
        )

        private val IOT_DEVICE_MODELS = setOf(
            CHProductModel.SSMTouch,
            CHProductModel.SSMTouch2,
            CHProductModel.SSMTouchPro,
            CHProductModel.SSMTouch2Pro,
            CHProductModel.SSMFace,
            CHProductModel.SSMFace2,
            CHProductModel.SSMFaceAI,
            CHProductModel.SSMFace2AI,
            CHProductModel.SSMFacePro,
            CHProductModel.SSMFace2Pro,
            CHProductModel.SSMFaceProAI,
            CHProductModel.SSMFace2ProAI
        )

        private const val TOPIC_OPENSENSOR_PREFIX = "opensensor/"
    }

    override fun addEventHandler(handler: CHEventHandler) {
        eventHandlers.add(handler)
    }

    override fun removeEventHandler(handler: CHEventHandler) {
        eventHandlers.remove(handler)
    }

    override fun clearEventHandlers() {
        eventHandlers.clear()
    }

    override fun getSSM2KeysLiveData(): LiveData<Map<String, ByteArray>>? {
        return (ssm2KeysMap as? ObservableMutableMap)?.liveData
    }

    override fun getSSM2SlotFullLiveData(): LiveData<Event<Boolean>>? {
        return (ssm2KeysMap as? ObservableMutableMap)?.slotFullLiveData
    }

    override fun getSSM2SupportLiveDataLiveData(): LiveData<Event<Boolean>>? {
        return (ssm2KeysMap as? ObservableMutableMap)?.supportLiveData
    }

    private fun reportBatteryData(payloadString: String) {
        L.d("harry", "[stp][reportBatteryData]:${isInternetAvailable()}, ${!isConnectedByWM2}, payload: $payloadString")
        CHAPIClientBiz.postBatteryData(deviceId.toString().uppercase(), payloadString) {
            it.onSuccess { resp ->
                batteryPercentage = ((resp.data as? Map<*, *>)?.get("batteryPercentage") as? Number)?.toInt()
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun onGattSesamePublish(receivePayload: SSM3PublishPayload) {
        L.d("harry", "[stp][onGattSesamePublish] cmdItCode: ${receivePayload.cmdItCode}  payload: ${receivePayload.payload.toHexString()}")
        super.onGattSesamePublish(receivePayload)

        var handled = false
        when (receivePayload.cmdItCode) {
            SesameItemCode.mechStatus.value -> {
                handleMechStatus(receivePayload)
                handled = true
            }

            SesameItemCode.PUB_KEY_SESAME.value -> {
                handled = true
                handlePubKeySesame(receivePayload)
            }

            SesameItemCode.SSM_OS3_RADAR_PARAM_PUBLISH.value -> {
                handled = true
                handleRadar(receivePayload)
            }

            SesameItemCode.versionTag.value -> {
                L.d("versionTag", "get SesameItemCode.versionTag...")
            }

            SesameItemCode.SSM3_ITEM_CODE_BATTERY_VOLTAGE.value -> {
                reportBatteryData(receivePayload.payload.toHexString())
            }

            SesameItemCode.SSM3_ITEM_CODE_SESAME_UNSUPPORT.value -> {
                handled = true
                (ssm2KeysMap as? ObservableMutableMap)?.setSupport(false)
            }

            SesameItemCode.SSM3_ITEM_CODE_BLE_TX_POWER_SETTING.value -> {
                handled = true
                bleTxPower = receivePayload.payload[0]
            }
        }

        if (!handled) {
            eventHandlers.forEach {
                if (it.handleEvent(this, receivePayload)) {
                    handled = true
                    return@forEach
                }
            }
        }
    }

    private fun handleRadar(receivePayload: SSM3PublishPayload) {
        (delegate as? CHDeviceConnectDelegate)?.onRadarReceive(this, receivePayload.payload)
    }

    private fun handleMechStatus(receivePayload: SSM3PublishPayload) {
        mechStatus = CHSesameTouchProMechStatus(receivePayload.payload)
        reportBatteryData(receivePayload.payload.sliceArray(0..1).toHexString())
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun handlePubKeySesame(receivePayload: SSM3PublishPayload) {
        L.d("hcia", "[ds][PUB][KEY]===>:${receivePayload.payload.toHexString()}")
        ssm2KeysMap.clear()
        val keyDatas = receivePayload.payload.divideArray(23)

        // 针对特殊机型opensenor系列，保留一个槽位给hub3，所以需要超过1个空槽位才算有空余
        val hasEmptySlot =
            if (productModel == CHProductModel.SSMOpenSensor || productModel == CHProductModel.SSMOpenSensor2) {
                keyDatas.count { it.all { byte -> byte == 0x00.toByte() } } > 1
            } else {
                keyDatas.any { it.all { byte -> byte == 0x00.toByte() } }
            }

        (ssm2KeysMap as? ObservableMutableMap)?.setSlotFull(!hasEmptySlot)

        keyDatas.forEach {
            val lockStatus = it[22].toInt()
            if (lockStatus != 0) {
                if (it[21].toInt() == 0x00) {
                    val ss5Id = it.sliceArray(IntRange(0, 15))
                    val ssmID = ss5Id.toHexString().noHashtoUUID().toString()
                    ssm2KeysMap[ssmID] = byteArrayOf(0x05, it[22])
                } else {
                    val ss2ir22 = it.sliceArray(IntRange(0, 21))
                    try {
                        val ssmID = (String(ss2ir22) + "==").base64decodeHex().noHashtoUUID().toString()
                        ssm2KeysMap[ssmID] = byteArrayOf(0x04, it[22])
                    } catch (e: Exception) {
                        L.d("hcia", "🩰  e:$e")
                    }
                }
            }
        }

        L.d("ssm2KeysMap", "[TPO][ssm2KeysMap] size=${ssm2KeysMap.size}  data=$ssm2KeysMap")
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun login(token: String?) {
        deviceStatus = CHDeviceStatus.BleLogining
        val sessionAuth: ByteArray? = if (isNeedAuthFromServer == true) {
            token?.hexStringToByteArray()
        } else {
            AesCmac(sesame2KeyData!!.secretKey.hexStringToByteArray(), 16).computeMac(mSesameToken)
        }
        cipher = SesameOS3BleCipher(
            "customDeviceName",
            sessionAuth!!,
            ("00" + mSesameToken.toHexString()).hexStringToByteArray()
        )
        sendCommand(
            SesameOS3Payload(SesameItemCode.login.value, sessionAuth.sliceArray(0..3)),
            DeviceSegmentType.plain
        ) {
            deviceStatus = CHDeviceStatus.Unlocked
            L.d("hcia", "[touchpro][login]][ok]:")
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun register(result: CHResult<CHEmpty>) {
        if (deviceStatus != CHDeviceStatus.ReadyToRegister) {
            result.invoke(Result.failure(NSError("Busy", "CBCentralManager", 7)))
            return
        }

        deviceStatus = CHDeviceStatus.Registering
        val serverSecret = mSesameToken.toHexString()

        CHAPIClientBiz.myDevicesRegisterSesame5Post(
            deviceId.toString(),
            CHOS3RegisterReq(productModel.productType().toString(), serverSecret)
        ) { apiResult ->
            apiResult.exceptionOrNull()?.let { e ->
                L.d("hcia", "[ss5][register][server] failed: ${e.message}")
            }

            sendCommand(
                SesameOS3Payload(
                    SesameItemCode.registration.value,
                    EccKey.getPubK().hexStringToByteArray() + System.currentTimeMillis().toUInt32ByteArray()
                ),
                DeviceSegmentType.plain
            ) { registerRes ->
                isRegistered = true
                val ecdhSecretPre16 = EccKey.ecdh(registerRes.payload).sliceArray(0..15)
                val wm2Key = ecdhSecretPre16.toHexString()
                val candyDevice = CHDevice(
                    deviceId.toString(),
                    advertisement!!.productModel!!.deviceModel(),
                    null,
                    "0000",
                    wm2Key,
                    serverSecret
                )
                sesame2KeyData = candyDevice
                deviceStatus = CHDeviceStatus.Unlocked
                val sessionAuth = AesCmac(ecdhSecretPre16, 16).computeMac(mSesameToken)
                cipher = SesameOS3BleCipher(
                    "customDeviceName",
                    sessionAuth!!,
                    ("00" + mSesameToken.toHexString()).hexStringToByteArray()
                )
                ssm2KeysMap.clear()
                CHDB.CHSS2Model.insert(candyDevice) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                }
            }
        }
    }

    override fun setBleTxPower(txPower: Byte, result: CHResult<CHEmpty>) {
        if (!isBleAvailable(result)) return
        sendCommand(
            SesameOS3Payload(
                SesameItemCode.SSM3_ITEM_CODE_BLE_TX_POWER_SETTING.value,
                byteArrayOf(txPower)
            ),
            DeviceSegmentType.cipher
        ) {}
    }

    override fun goIOT() {
        when {
            productModel in IOT_BATTERY_OPENSENSOR_MODELS -> {
                subscribeOpensensorTopic()
            }

            productModel in IOT_DEVICE_MODELS -> {
                subscribeDeviceShadow()
            }
        }
    }

    private fun subscribeOpensensorTopic() {
        val topic = "$TOPIC_OPENSENSOR_PREFIX${deviceId.toString().uppercase()}"
        L.d(tag, "Subscribing to opensensor topic: $productModel || $topic")
        CHIotManager.subscribeTopic(topic) { result ->
            result
                .onSuccess { data ->
                    processOpensensorData(data.data)
                }
                .onFailure { error ->
                    L.e(tag, "Failed to subscribe opensensor topic: ${error.message}")
                }
        }
    }

    private fun processOpensensorData(data: ByteArray) {
        try {
            mechStatus = CHSesameOpenSensorMechStatus(OpenSensorData.fromByteArray(data))
            L.d(tag, "OpenSensor $productModel")
        } catch (e: Exception) {
            L.e(tag, "Failed to parse unified data: ${e.message}")
        }
    }

    private fun subscribeDeviceShadow() {
        CHIotManager.subscribeSesame2Shadow(this) { result ->
            result
                .onSuccess { resource ->
                    L.d(tag, "[$productModel] resource.data.state.reported:${resource.data.state.reported.wm2s}")
                    resource.data.state.reported.apply {
                        val connected = wm2s?.hasWM2Connection() ?: false
                        L.d(tag, "[isConnectedByWM2: $connected]")
                        deviceShadowStatus = if (connected) CHDeviceStatus.IotConnected else null
                    }
                }
                .onFailure { error ->
                    L.e(tag, "Failed to subscribe device shadow: ${error.message}")
                }
        }
    }

    override fun insertSesame(sesame: CHDevices, result: CHResult<CHEmpty>) {
        deviceConnectCapability.insertSesame(sesame, result)
    }

    override fun removeSesame(tag: String, result: CHResult<CHEmpty>) {
        deviceConnectCapability.removeSesame(tag, result)
    }

    override fun <T> isBleAvailable(result: CHResult<T>): Boolean {
        return (this as CHDevices).isBleAvailable(result)
    }

    override fun sendCommand(payload: SesameOS3Payload, callback: (SSM3ResponsePayload) -> Unit) {
        super.sendCommand(payload, DeviceSegmentType.cipher, callback)
    }

    override fun setRadarSensitivity(payload: ByteArray, result: CHResult<CHEmpty>) {
        deviceConnectCapability.setRadarSensitivity(payload, result)
    }

    private fun Map<*, String>.hasWM2Connection(): Boolean =
        any { (_, value) -> value.hexStringToByteArray().firstOrNull()?.toInt() == 1 }
}

class ObservableMutableMap<K, V> : MutableMap<K, V> {
    private val map = mutableMapOf<K, V>()
    private val _liveData = MutableLiveData<Map<K, V>>(map)
    val liveData: LiveData<Map<K, V>> = _liveData
    private val _slotFullLiveData = MutableLiveData<Event<Boolean>>()
    val slotFullLiveData: LiveData<Event<Boolean>> = _slotFullLiveData
    private val _supportLiveData = MutableLiveData<Event<Boolean>>()
    val supportLiveData: LiveData<Event<Boolean>> = _supportLiveData

    fun setSlotFull(isFull: Boolean) {
        _slotFullLiveData.postValue(Event(isFull))
    }

    fun setSupport(isSupport: Boolean) {
        _supportLiveData.postValue(Event(isSupport))
    }

    override fun put(key: K, value: V): V? {
        val result = map.put(key, value)
        _liveData.postValue(HashMap(map))
        return result
    }

    override fun clear() {
        map.clear()
        _liveData.postValue(HashMap(map))
    }

    override fun remove(key: K): V? {
        val result = map.remove(key)
        _liveData.postValue(HashMap(map))
        return result
    }

    override fun putAll(from: Map<out K, V>) {
        map.putAll(from)
        _liveData.postValue(HashMap(map))
    }

    override val size: Int get() = map.size
    override fun containsKey(key: K) = map.containsKey(key)
    override fun containsValue(value: V) = map.containsValue(value)
    override fun get(key: K) = map[key]
    override fun isEmpty() = map.isEmpty()
    override val entries get() = map.entries
    override val keys get() = map.keys
    override val values get() = map.values
}