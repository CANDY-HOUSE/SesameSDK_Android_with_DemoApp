package co.candyhouse.sesame.open.device.sesameBiometric.devices

import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.CHadv
import co.candyhouse.sesame.ble.DeviceSegmentType
import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.SSM3ResponsePayload
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.isBleAvailable
import co.candyhouse.sesame.ble.os3.CHRemoteNanoTriggerSettings
import co.candyhouse.sesame.ble.os3.base.CHSesameOS3
import co.candyhouse.sesame.ble.os3.base.SesameOS3BleCipher
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.open.CHAccountManager
import co.candyhouse.sesame.open.CHAccountManager.makeApiCall
import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.open.CHResultState
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.CHSesameOpenSensorMechStatus
import co.candyhouse.sesame.open.device.CHSesameTouchProMechStatus
import co.candyhouse.sesame.open.device.NSError
import co.candyhouse.sesame.open.device.OpenSensorData
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHCapabilitySupport
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHEventHandler
import co.candyhouse.sesame.open.device.sesameBiometric.capability.card.CHCardDelegate
import co.candyhouse.sesame.open.device.sesameBiometric.capability.connect.CHDeviceConnectCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.connect.CHDeviceConnectCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.capability.connect.CHDeviceConnectDelegate
import co.candyhouse.sesame.open.device.sesameBiometric.capability.remoteNano.CHRemoteNanoCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.remoteNano.CHRemoteNanoCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.capability.remoteNano.CHRemoteNanoDelegate
import co.candyhouse.sesame.server.CHIotManager
import co.candyhouse.sesame.server.dto.CHCardNameRequest
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.server.dto.CHOS3RegisterReq
import co.candyhouse.sesame.utils.EccKey
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame.utils.base64decodeHex
import co.candyhouse.sesame.utils.divideArray
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.noHashtoUUID
import co.candyhouse.sesame.utils.toHexString
import co.candyhouse.sesame.utils.toUInt32ByteArray

internal open class CHSesameBiometricBaseDevice : CHSesameOS3(), CHSesameBiometricBase, CHCapabilitySupport, CHDeviceUtil, CHRemoteNanoCapable by CHRemoteNanoCapableImpl() {

    // 基本属性
    override var ssm2KeysMap: MutableMap<String, ByteArray> = mutableMapOf()
    override var advertisement: CHadv? = null
        set(value) {
            field = value
            parceADV(value)
            value?.let {
                // 保留广播通信相关处理
            }
        }

    override var radarPayload: ByteArray = ByteArray(0)
        set(value) {
            field = value
            value.let {}
        }

    override var triggerDelaySetting: CHRemoteNanoTriggerSettings? = null

    // 是否通过WM2连接
    protected var isConnectedByWM2: Boolean = false

    // 事件处理器列表
    private val eventHandlers = mutableListOf<CHEventHandler>()

    // 设备连接能力实现
    private val deviceConnectCapability: CHDeviceConnectCapable

    init {
        deviceConnectCapability = CHDeviceConnectCapableImpl()
        deviceConnectCapability.setSupport(this)
    }

    /**
     * 添加事件处理器
     */
    override fun addEventHandler(handler: CHEventHandler) {
        eventHandlers.add(handler)
    }

    /**
     * 移除事件处理器
     */
    override fun removeEventHandler(handler: CHEventHandler) {
        eventHandlers.remove(handler)
    }

    /**
     * 清除所有事件处理器
     */
    override fun clearEventHandlers() {
        eventHandlers.clear()
    }

    /**
     * 处理来自设备的事件通知
     */
    @OptIn(ExperimentalStdlibApi::class)
    override fun onGattSesamePublish(receivePayload: SSM3PublishPayload) {
        super.onGattSesamePublish(receivePayload)
        // 处理机械状态更新
        var handled = false
        when (receivePayload.cmdItCode) {
//            SesameItemCode.REMOTE_NANO_ITEM_CODE_PUB_TRIGGER_DELAYTIME.value -> {
//                handleTriggerDelayTime(receivePayload)
//                handled = true
//            }
            SesameItemCode.mechStatus.value -> {
                handleMechStatus(receivePayload)
                handled = true
            }

            SesameItemCode.PUB_KEY_SESAME.value -> {
                handled = true
                handlePubKeySesame(receivePayload)
            }

            SesameItemCode.SSM_OS3_AT58LPB_PARAM_PUBLISH.value -> {
                L.e("sf", "get SesameItemCode.SSM_OS3_AT58LPB_PARAM_PUBLISH...")
                handled = true
                handleRadar(receivePayload)
            }
        }

        // 分发事件到各处理器
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
        radarPayload = receivePayload.payload
        (delegate as? CHDeviceConnectDelegate)?.onRadarReceive(this, receivePayload.payload)
    }

    private fun handleTriggerDelayTime(receivePayload: SSM3PublishPayload) {
        triggerDelaySetting = CHRemoteNanoTriggerSettings.fromData(receivePayload.payload)
        L.d("hcia", "[stp][REMOTE_NANO_ITEM_CODE_PUB_TRIGGER_DELAYTIME]" + triggerDelaySetting!!.triggerDelaySecond.toString())
        triggerDelaySetting?.let {
            (delegate as? CHRemoteNanoDelegate)?.onTriggerDelaySecondReceived(
                this, it
            )
        }
    }

    private fun handleMechStatus(receivePayload: SSM3PublishPayload) {
        mechStatus = CHSesameTouchProMechStatus(receivePayload.payload)
        L.d("hcia", "getBatteryPrecentage: ${mechStatus?.getBatteryPrecentage()}  getBatteryVoltage: ${mechStatus?.getBatteryVoltage()}")
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun handlePubKeySesame(receivePayload: SSM3PublishPayload) {
        L.d("hcia", "[ds][PUB][KEY]===>:" + receivePayload.payload.toHexString())
        ssm2KeysMap.clear()
        val keyDatas = receivePayload.payload.divideArray(23)
        keyDatas.forEach {
            val lock_status = it[22].toInt()
            if (lock_status != 0) {
                if (it[21].toInt() == 0x00) {
                    val ss5_id = it.sliceArray(IntRange(0, 15))
                    val ssmID = ss5_id.toHexString().noHashtoUUID().toString()
                    ssm2KeysMap.put(ssmID, byteArrayOf(0x05, it[22]))
                } else {
                    val ss2_ir_22 = it.sliceArray(IntRange(0, 21))
                    try {
                        val ssmID = (String(ss2_ir_22) + "==").base64decodeHex().noHashtoUUID().toString()
                        ssm2KeysMap.put(ssmID, byteArrayOf(0x04, it[22]))
                    } catch (e: Exception) {
                        L.d("hcia", "🩰  e:" + e)
                    }
                }
            }
        }
        L.d("hcia", "[TPO][ssm2KeysMap] " + ssm2KeysMap)
        L.d("hcia", "[TPO][ssm2KeysMap] size=${ssm2KeysMap.size}  data=" + ssm2KeysMap)
        (delegate as? CHDeviceConnectDelegate)?.onSSM2KeysChanged(this, ssm2KeysMap)
    }

    /**
     * 登录设备
     */
    @OptIn(ExperimentalStdlibApi::class)
    override fun login(token: String?) {
        deviceStatus = CHDeviceStatus.BleLogining

        val sessionAuth: ByteArray? = if (isNeedAuthFromServer == true) {
            token?.hexStringToByteArray()
        } else {
            AesCmac(sesame2KeyData!!.secretKey.hexStringToByteArray(), 16).computeMac(mSesameToken)
        }

        cipher = SesameOS3BleCipher("customDeviceName", sessionAuth!!, ("00" + mSesameToken.toHexString()).hexStringToByteArray())

        sendCommand(SesameOS3Payload(SesameItemCode.login.value, sessionAuth!!.sliceArray(0..3)), DeviceSegmentType.plain) { res ->
            deviceStatus = CHDeviceStatus.Unlocked
            L.d("hcia", "[touchpro][login]][ok]:")
        }
    }

    /**
     * 注册设备
     */
    @OptIn(ExperimentalStdlibApi::class)
    override fun register(result: CHResult<CHEmpty>) {
        L.d("harry", "TouchPro register")

        if (deviceStatus != CHDeviceStatus.ReadyToRegister) {
            result.invoke(Result.failure(NSError("Busy", "CBCentralManager", 7)))
            return
        }

        deviceStatus = CHDeviceStatus.Registering

        makeApiCall(result) {
            val serverSecret = mSesameToken.toHexString()
            CHAccountManager.jpAPIclient.myDevicesRegisterSesame5Post(
                deviceId.toString(), CHOS3RegisterReq(productModel.productType().toString(), serverSecret)
            )

            deviceStatus = CHDeviceStatus.Registering

            sendCommand(
                SesameOS3Payload(
                    SesameItemCode.registration.value, EccKey.getPubK().hexStringToByteArray() + System.currentTimeMillis().toUInt32ByteArray()
                ), DeviceSegmentType.plain
            ) { IRRes ->
                isRegistered = true
                val ecdhSecretPre16 = EccKey.ecdh(IRRes.payload).sliceArray(0..15)
                val wm2Key = ecdhSecretPre16.toHexString()

                val candyDevice = CHDevice(
                    deviceId.toString(), advertisement!!.productModel!!.deviceModel(), null, "0000", wm2Key, serverSecret
                )

                sesame2KeyData = candyDevice
                deviceStatus = CHDeviceStatus.Unlocked

                val sessionAuth = AesCmac(ecdhSecretPre16, 16).computeMac(mSesameToken)
                cipher = SesameOS3BleCipher(
                    "customDeviceName", sessionAuth!!, ("00" + mSesameToken.toHexString()).hexStringToByteArray()
                )

                ssm2KeysMap.clear()

                CHDB.CHSS2Model.insert(candyDevice) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                }

                sendCommand(SesameOS3Payload(SesameItemCode.mechStatus.value, byteArrayOf()), DeviceSegmentType.cipher) { res ->
                    mechStatus = CHSesameTouchProMechStatus(res.payload)
                    L.d("harry", "[reg]getBatteryPrecentage: " + mechStatus?.getBatteryPrecentage())
                    L.d("harry", "[reg]getBatteryVoltage: " + mechStatus?.getBatteryVoltage())
                }
            }
        }
    }

    /**
     * 连接到IoT服务
     */
    override fun goIOT() {
        L.d("hcia", "[stp]goIOT:")

        if (productModel === CHProductModel.SSMOpenSensor) {
            goIoTWithOpenSensor()
            return
        }

        CHIotManager.subscribeSesame2Shadow(this) { result ->
            result.onSuccess { resource ->
                L.d("hcia", "[stp][iot]")
                L.d("hcia", "resource.data.state.reported.wm2s:" + resource.data.state.reported.wm2s)

                resource.data.state.reported.wm2s?.let { wm2s ->
                    isConnectedByWM2 = wm2s.map { it.value.hexStringToByteArray().first().toInt() }.contains(1)
                    L.d("hcia", "[ss5][wm2s: $wm2s][isConnectedByWM2: $isConnectedByWM2]")
                }

                if (isConnectedByWM2) {
                    deviceShadowStatus = CHDeviceStatus.IotConnected
                } else {
                    deviceShadowStatus = null
                }
            }
        }


    }

    override fun subscribeNameUpdateTopic(cardDelegate: CHCardDelegate) {
        CHIotManager.subscribeNameUpdateTopic(this) {
            it.onSuccess { resource ->
                L.d("harry", "[stp][iot][nameUpdate]:" + resource.data)
                val cardNameRequest: CHCardNameRequest = resource.data
                cardDelegate.onCardReceive(this as CHSesameConnector, cardNameRequest.cardID, cardNameRequest.name.toByteArray().toHexString(), cardNameRequest.cardType)
            }
        }
    }

    override fun unsubscribeNameUpdateTopic() {
        CHIotManager.unsubscribeNameUpdateTopic(this)
    }

    /**
     * 处理OpenSensor的IoT连接
     */
    private fun goIoTWithOpenSensor() {
        if (productModel != CHProductModel.SSMOpenSensor) return

        L.d("hcia", "[stp]goIoTWithOpenSensor:")
        val topic = "opensensor/${deviceId.toString().uppercase()}"

        CHIotManager.subscribeTopic(topic) {
            it.onSuccess {
                val openSensorData = OpenSensorData.fromByteArray(it.data)
                mechStatus = CHSesameOpenSensorMechStatus(openSensorData)
            }
        }

        getLatestState {
            it.onSuccess { result ->
                mechStatus = CHSesameOpenSensorMechStatus(result.data)
            }
        }
    }

    /**
     * 获取最新状态
     */
    private fun getLatestState(result: CHResult<OpenSensorData>) {
        CHAccountManager.openSensorHistoryGet(sesame2KeyData!!.deviceUUID.uppercase()) { it ->
            L.d("harry", "openSensorHistoryGet: $it")
            it.onSuccess {
                val openSensorData = OpenSensorData.fromByteArray(it.data.toString().toByteArray())
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(openSensorData)))
            }
        }
    }

    /**
     * 插入Sesame设备
     */
    override fun insertSesame(sesame: CHDevices, result: CHResult<CHEmpty>) {
        deviceConnectCapability.insertSesame(sesame, result)
    }

    /**
     * 移除Sesame设备
     */
    override fun removeSesame(tag: String, result: CHResult<CHEmpty>) {
        deviceConnectCapability.removeSesame(tag, result)
    }

    /**
     * 检查蓝牙可用性
     */
    override fun <T> isBleAvailable(result: CHResult<T>): Boolean {
       return  (this as CHDevices).isBleAvailable(result)
    }

    /**
     * 发送命令
     */
    override fun sendCommand(payload: SesameOS3Payload, callback: (SSM3ResponsePayload) -> Unit) {
        super.sendCommand(payload, DeviceSegmentType.cipher, callback)
    }

    /**
     * 设置雷达灵敏度
     */
    override fun setRadarSensitivity(payload: ByteArray, result: CHResult<CHEmpty>) {
        deviceConnectCapability.setRadarSensitivity(payload, result)
    }

}

