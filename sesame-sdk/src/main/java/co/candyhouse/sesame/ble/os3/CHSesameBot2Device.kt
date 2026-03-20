package co.candyhouse.sesame.ble.os3

import android.annotation.SuppressLint
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.CHadv
import co.candyhouse.sesame.ble.DeviceSegmentType
import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.SesameResultCode
import co.candyhouse.sesame.ble.isBleAvailable
import co.candyhouse.sesame.ble.os3.base.CHSesameOS3
import co.candyhouse.sesame.ble.os3.base.SesameOS3BleCipher
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.db.model.historyTagBLE
import co.candyhouse.sesame.db.model.historyTagIOT
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHSesame2MechStatus
import co.candyhouse.sesame.open.device.CHSesame5MechStatus
import co.candyhouse.sesame.open.device.CHSesameBike2MechStatus
import co.candyhouse.sesame.open.device.CHSesameBot2
import co.candyhouse.sesame.open.device.CHSesameBot2MechStatus
import co.candyhouse.sesame.open.device.CHSesamebot2Event
import co.candyhouse.sesame.open.device.CHSesamebot2Status
import co.candyhouse.sesame.open.device.NSError
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.CHIotManager
import co.candyhouse.sesame.server.dto.CHOS3RegisterReq
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.utils.EccKey
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.isInternetAvailable
import co.candyhouse.sesame.utils.toBigLong
import co.candyhouse.sesame.utils.toHexString
import co.candyhouse.sesame.utils.toUInt32ByteArray
import kotlin.math.abs

@SuppressLint("MissingPermission")
internal class CHSesameBot2Device : CHSesameOS3(), CHSesameBot2, CHDeviceUtil {
    /** 設備廣播 */
    override var advertisement: CHadv? = null
        set(value) {
            field = value
            parceADV(value)
            value?.let {
                isHistory = it.adv_tag_b1
            }
        }

    var isHistory: Boolean = false
        set(value) {
            if (deviceStatus.value == CHDeviceLoginStatus.logined) {
                field = value
                if (field) {
                    readHistoryCommand()
                }
            }
        }

    /** 聯網處理 */
    var isConnectedByWM2: Boolean = false
    var bot2Topic: String = ""
    override var scripts: CHSesamebot2Status =
        CHSesamebot2Status(curIdx = 0u, eventLength = 0u, events = emptyList())
    private val scriptNameListLock = Any()
    private var scriptNameListInFlight = false
    private val pendingScriptNameListResults = mutableListOf<CHResult<CHSesamebot2Status>>()

    override fun goIOT() {
        CHIotManager.subscribeSesame2Shadow(this) { result ->
            result.onSuccess { resource ->
                resource.data.state.reported.wm2s?.let { wm2s ->
                    bot2Topic = wm2s.keys.map { key ->
                        "wm2${key}cmd"
                    }.toString().removeSurrounding("[", "]")
                    isConnectedByWM2 = wm2s.map { it.value.hexStringToByteArray().first().toInt() }.contains(1)
                }
                if (isConnectedByWM2) {
                    resource.data.state.reported.mechst?.let { mechShadow ->
                        L.d("harry", "[bot2][iot]mechShadow[${mechShadow.hexStringToByteArray().size}]: $mechShadow")
                        if (mechShadow.hexStringToByteArray().size >= 7) {
                            // 新固件， mechStatus 统一成 SS5 格式 的 7个字节, IoT 返回 SS4 格式的 8 个字节
                            mechStatus = CHSesame5MechStatus(CHSesame2MechStatus(mechShadow.hexStringToByteArray()).ss5Adapter())
                        } else if (mechShadow.hexStringToByteArray().size == 3) {
                            // 旧固件， mechStatus 只有 3 个字节
                            mechStatus = CHSesameBot2MechStatus(mechShadow.hexStringToByteArray().sliceArray(0..2))
                        }
                    }
                    deviceShadowStatus = if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
                } else {
                    deviceShadowStatus = null
                }
            }
        }
    }

    /** 指令發送  */
    override fun click(index: UByte?, historytag: ByteArray?, result: CHResult<CHEmpty>) {
        L.d("hcia", "[bot2]click[index:$index]")
        var itemCode = SesameItemCode.click
        if (index != null) {
            itemCode = SesameItemCode.values().find { it.value == (SesameItemCode.BOT2_ITEM_CODE_RUN_SCRIPT_0.value + index).toUByte() }
                ?: SesameItemCode.click
        }
        if (deviceStatus.value == CHDeviceLoginStatus.unlogined && deviceShadowStatus != null) {
            CHAPIClientBiz.cmdSesame(itemCode, this, sesame2KeyData!!.historyTagIOT(historytag), result)
            return
        }
        if (!isBleAvailable(result)) {
            CHAPIClientBiz.cmdSesame(itemCode, this, sesame2KeyData!!.historyTagIOT(historytag), result)
            return
        }
        sendCommand(SesameOS3Payload(itemCode.value, sesame2KeyData!!.historyTagBLE(historytag)), DeviceSegmentType.cipher) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun sendClickScript(index: UByte, script: ByteArray, result: CHResult<CHEmpty>) {
        L.d("hcia", "[bot2]combinedData:" + index.toString() + script.toList())
        if (!isBleAvailable(result)) return
        var sendData = if (index != null) byteArrayOf(index.toByte()) + script else script
        sendCommand(SesameOS3Payload(SesameItemCode.BOT2_ITEM_CODE_EDIT_SCRIPT.value, sendData), DeviceSegmentType.cipher) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun selectScript(index: UByte, result: CHResult<CHEmpty>) {
        if (!isBleAvailable(result)) return
        L.d("selectScript", "[bot2]select[index:$index]")
        sendCommand(SesameOS3Payload(SesameItemCode.SCRIPT_SELECT.value, byteArrayOf(index.toByte())), DeviceSegmentType.cipher) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun getCurrentScript(index: UByte?, result: CHResult<CHSesamebot2Event>) {
        L.d("hcia", "[send]getNowScript")
        if (!isBleAvailable(result)) return
        var idx = if (index != null) byteArrayOf(index.toByte()) else byteArrayOf()
        L.d("hcia", idx.toString())
        sendCommand(SesameOS3Payload(SesameItemCode.SCRIPT_CURRENT.value, idx), DeviceSegmentType.cipher) { res ->
            L.d("hcia", "result" + res.payload.toList())
            if (res.cmdResultCode == SesameResultCode.success.value) {
                val resp = CHSesamebot2Event.fromByteArray(res.payload)
                if (resp != null) result.invoke(Result.success(CHResultState.CHResultStateBLE(resp))) else result.invoke(
                    Result.failure(
                        NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())
                    )
                )
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }

    override fun getScriptNameList(result: CHResult<CHSesamebot2Status>) {
        L.d("getScriptNameList", "[send]getScriptNameList")
        if (!isBleAvailable(result)) {
            result.invoke(Result.failure(NSError("ble_unavailable", "SCRIPT_NAME_LIST", -2)))
            return
        }
        synchronized(scriptNameListLock) {
            pendingScriptNameListResults.add(result)
            if (scriptNameListInFlight) {
                L.d("getScriptNameList", "getScriptNameList merged (inFlight)")
                return
            }
            scriptNameListInFlight = true
        }
        sendCommand(SesameOS3Payload(SesameItemCode.SCRIPT_NAME_LIST.value, byteArrayOf()), DeviceSegmentType.cipher) { res ->
            val callbacks: List<CHResult<CHSesamebot2Status>> = synchronized(scriptNameListLock) {
                scriptNameListInFlight = false
                val copy = pendingScriptNameListResults.toList()
                pendingScriptNameListResults.clear()
                copy
            }
            if (res.cmdResultCode == SesameResultCode.success.value) {
                val payload = res.payload
                L.d("getScriptNameList", "SCRIPT_NAME_LIST payload=${payload.toHexString()}")

                val status = CHSesamebot2Status.fromByteArray(payload)
                if (status != null) {
                    scripts = status
                    callbacks.forEach { cb ->
                        cb.invoke(Result.success(CHResultState.CHResultStateBLE(status)))
                    }
                } else {
                    callbacks.forEach { cb ->
                        cb.invoke(Result.failure(NSError("parse_failed", "SCRIPT_NAME_LIST", -1)))
                    }
                }
            } else {
                callbacks.forEach { cb ->
                    cb.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
                }
            }
        }
    }

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
                ), DeviceSegmentType.plain
            ) { IRRes ->
                var ecdhSecret = byteArrayOf()
                try {
                    val eccPublicKeyFromSS5 = IRRes.payload.toHexString().hexStringToByteArray().sliceArray(13..76)
                    ecdhSecret = EccKey.ecdh(eccPublicKeyFromSS5)
                } catch (e: Exception) {
                    // 如果 EccKey.ecdh 失败， 则可能是旧固件。 旧固件的 mechStatus 只有 3 个字节, 且不含 mechSetting
                    /** 根據設備狀態特殊處理 */
                    mechStatus = CHSesameBot2MechStatus(IRRes.payload.toHexString().hexStringToByteArray().sliceArray(0..2))
                    deviceStatus = if (mechStatus?.isInLockRange == true) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
                    val ecdhSecretPre16 = EccKey.ecdh(IRRes.payload.toHexString().hexStringToByteArray().sliceArray(3..66)).sliceArray(0..15)
                    sesame2KeyData =
                        CHDevice(deviceId.toString(), productModel.deviceModel(), null, "0000", ecdhSecretPre16.toHexString(), serverSecret)
                    cipher = SesameOS3BleCipher(
                        "customDeviceName",
                        AesCmac(ecdhSecretPre16, 16).computeMac(mSesameToken)!!,
                        ("00" + mSesameToken.toHexString()).hexStringToByteArray()
                    )
                    CHDB.CHSS2Model.insert(sesame2KeyData!!) {
                        result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                    }
                    return@sendCommand
                }
                // 新固件统一成 SS5 一样的格式
                // TODO： 重构代码，把 bike2 和 bot2 合并为 CHSesame5Device
                mechStatus = CHSesame5MechStatus(IRRes.payload.toHexString().hexStringToByteArray().sliceArray(0..6))
                val ecdhSecretPre16 = ecdhSecret.sliceArray(0..15)
                val deviceSecret = ecdhSecretPre16.toHexString()
                val candyDevice =
                    CHDevice(deviceId.toString(), advertisement!!.productModel!!.deviceModel(), null, "0000", deviceSecret, serverSecret)
                sesame2KeyData = candyDevice
                val sessionAuth = AesCmac(ecdhSecretPre16, 16).computeMac(mSesameToken)
                cipher = SesameOS3BleCipher("customDeviceName", sessionAuth!!, ("00" + mSesameToken.toHexString()).hexStringToByteArray())
                CHDB.CHSS2Model.insert(candyDevice) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                }
                deviceStatus = if (mechStatus?.isInLockRange == true) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
            }
        }
    }

    override fun login(token: String?) {
        deviceStatus = CHDeviceStatus.BleLogining
        val sessionAuth: ByteArray? = if (isNeedAuthFromServer == true) {
            token?.hexStringToByteArray()
        } else {
            AesCmac(sesame2KeyData!!.secretKey.hexStringToByteArray(), 16).computeMac(mSesameToken)
        }
        cipher = SesameOS3BleCipher("customDeviceName", sessionAuth!!, ("00" + mSesameToken.toHexString()).hexStringToByteArray())
        sendCommand(
            SesameOS3Payload(SesameItemCode.login.value, sessionAuth.sliceArray(0..3)),
            DeviceSegmentType.plain
        ) { loginPayload ->
            val systemTime = loginPayload.payload.sliceArray(0..3).toBigLong()
            val currentTimestamp = System.currentTimeMillis() / 1000
            val timeMinus = currentTimestamp.minus(systemTime)

            if (abs(timeMinus) > 3) {
                sendCommand(SesameOS3Payload(SesameItemCode.time.value, System.currentTimeMillis().toUInt32ByteArray()), DeviceSegmentType.cipher) {}
            }
        }
    }

    private fun readHistoryCommand() {
        val isConnectNET = isInternetAvailable()
        sendCommand(SesameOS3Payload(SesameItemCode.history.value, byteArrayOf(0x01)), DeviceSegmentType.cipher) { res ->
            L.d("readHistoryCommand", "[ResultCode]:" + res.cmdResultCode)
            val hisPaylaod = res.payload
            L.d("readHistoryCommand", "[hisPaylaod]:${this.deviceId.toString().uppercase()} ${hisPaylaod.toHexString()}")
            onHistoryReceived(hisPaylaod)
            if (res.cmdResultCode == SesameResultCode.success.value) {
                if (isConnectNET && !isConnectedByWM2) {
                    CHAPIClientBiz.postOS3History(deviceId.toString().uppercase(), hisPaylaod.toHexString()) {
                        // 成功上传历史记录到云端后， 通过蓝牙删除这条历史记录， 固件会在它的Flash里删除掉这条历史记录。
                        val recordId = hisPaylaod.sliceArray(0..3)
                        it.onSuccess {
                            L.d("readHistoryCommand", "[+]SSM2_ITEM_CODE_HISTORY_DELETE: ${recordId.toBigLong().toInt()}")
                            sendCommand(
                                SesameOS3Payload(SesameItemCode.SSM2_ITEM_CODE_HISTORY_DELETE.value, recordId),
                                DeviceSegmentType.cipher
                            ) { res ->
                                L.d("readHistoryCommand", "[-]SSM2_ITEM_CODE_HISTORY_DELETE: ${res.cmdResultCode}")
                            }
                        }
                        it.onFailure { exception ->
                            L.d("readHistoryCommand", "[history]postSS5History: $exception")
                        }
                    }
                }
            }
        }
    }

    override fun setBleTxPower(txPower: Byte, result: CHResult<CHEmpty>) {
        if (!isBleAvailable(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM3_ITEM_CODE_BLE_TX_POWER_SETTING.value, byteArrayOf(txPower)), DeviceSegmentType.cipher) { res ->
        }
    }

    private fun reportBatteryData(payloadString: String) {
        L.d("harry", "[ss5][reportBatteryData]:" + isInternetAvailable() + ", " + !isConnectedByWM2 + ", payload: " + payloadString)
        CHAPIClientBiz.postBatteryData(deviceId.toString().uppercase(), payloadString) {
            it.onSuccess { resp ->
                batteryPercentage = ((resp.data as? Map<*, *>)?.get("batteryPercentage") as? Number)?.toInt()
            }
        }
    }

    /** 指令接收 */
    override fun onGattSesamePublish(receivePayload: SSM3PublishPayload) {
        L.d("harry", "onGattSesamePublish: ${receivePayload.cmdItCode} payload: ${receivePayload.payload.toHexString()}")
        super.onGattSesamePublish(receivePayload)
        if (receivePayload.cmdItCode == SesameItemCode.SSM3_ITEM_CODE_BATTERY_VOLTAGE.value) {
            reportBatteryData(receivePayload.payload.toHexString())
        }
        if (receivePayload.cmdItCode == SesameItemCode.mechStatus.value) {
            L.d("harry", "[bot2]mechStatus【size: ${receivePayload.payload.size}】: ${receivePayload.payload.toHexString()}")
            if (receivePayload.payload.size == 7) {
                // 新固件， mechStatus 统一成 SS5 格式 的 7个字节
                mechStatus = CHSesame5MechStatus(receivePayload.payload)
            } else if (receivePayload.payload.size == 3) {
                // 旧固件， mechStatus 只有 3 个字节
                mechStatus = CHSesameBike2MechStatus(receivePayload.payload)
            }
            deviceStatus = if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
            reportBatteryData(receivePayload.payload.sliceArray(0..1).toHexString())
        }
    }
}