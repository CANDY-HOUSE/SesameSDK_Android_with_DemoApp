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
import co.candyhouse.sesame.open.CHAccountManager
import co.candyhouse.sesame.open.CHAccountManager.makeApiCall
import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.open.CHResultState
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
import co.candyhouse.sesame.open.isInternetAvailable
import co.candyhouse.sesame.server.CHIotManager
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.server.dto.CHOS3RegisterReq
import co.candyhouse.sesame.utils.EccKey
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.toHexString
import co.candyhouse.sesame.utils.toUInt32ByteArray

@SuppressLint("MissingPermission")
internal class CHSesameBot2Device : CHSesameOS3(), CHSesameBot2, CHDeviceUtil {
    /** 設備廣播 */
    override var advertisement: CHadv? = null
        set(value) {
            field = value
            parceADV(value)
        }


    /** 聯網處理 */
    var isConnectedByWM2: Boolean = false
    var bot2Topic: String = ""
    override var scripts: CHSesamebot2Status = run {
        val events = mutableListOf<CHSesamebot2Event>()
        for (i in 0..9) {
            events.add(CHSesamebot2Event(nameLength = 1u, name = byteArrayOf((48 + i).toByte())))
        }
        CHSesamebot2Status(curIdx = 0u, eventLength = 10u, events = events)
    }
    override fun goIOT() {
        // L.d("hcia", "[bot2]goIOT:")
        CHIotManager.subscribeSesame2Shadow(this) { result ->
            result.onSuccess { resource ->
                // L.d("hcia", "[bot2][iot]")
                // L.d("hcia", "\uD83E\uDD5D [bot2]ss5_shadow裡存的hub3列表:" + resource.data.state.reported.wm2s)
                resource.data.state.reported.wm2s?.let { wm2s ->
                    bot2Topic = wm2s.keys.map { key ->
                        "wm2${key}cmd"
                    }.toString().removeSurrounding("[", "]")
                    isConnectedByWM2 = wm2s.map { it.value.hexStringToByteArray().first().toInt() }.contains(1)
                }
                if (isConnectedByWM2) {
                    resource.data.state.reported.mechst?.let { mechShadow ->
                        L.d("harry", "[bot2][iot]mechShadow[${mechShadow.hexStringToByteArray().size}]: $mechShadow")
                        if(mechShadow.hexStringToByteArray().size >= 7){
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
    override fun click(index: UByte?, result: CHResult<CHEmpty>) {
        L.d("hcia", "[bot2]click[index:$index]")
        var itemCode = SesameItemCode.click
        if (index != null) {
            itemCode = SesameItemCode.values().find { it.value == (SesameItemCode.BOT2_ITEM_CODE_RUN_SCRIPT_0.value + index).toUByte() } ?: SesameItemCode.click
        }
        if (deviceStatus.value == CHDeviceLoginStatus.unlogined && deviceShadowStatus != null) {
            CHAccountManager.cmdSesame(itemCode, this, byteArrayOf(), result)
            return
        }
        if (!isBleAvailable(result)) {
            CHAccountManager.cmdSesame(itemCode, this, byteArrayOf(), result)
            return
        }
        sendCommand(SesameOS3Payload(itemCode.value, byteArrayOf()), DeviceSegmentType.cipher) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun sendClickScript(index: UByte, script: ByteArray, result: CHResult<CHEmpty>) {
         L.d("hcia", "[bot2]combinedData:" + index.toString() + script.toList())
        if (!isBleAvailable(result)) return
        var sendData = if (index != null) byteArrayOf(index.toByte()) + script  else script
        sendCommand(SesameOS3Payload(SesameItemCode.BOT2_ITEM_CODE_EDIT_SCRIPT.value, sendData), DeviceSegmentType.cipher) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun selectScript(index: UByte, result: CHResult<CHEmpty>) {
        // L.d("hcia", "[send]selectScript:" + index)
        result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
    }

    override fun getCurrentScript(index: UByte?, result: CHResult<CHSesamebot2Event>) {
        L.d("hcia", "[send]getNowScript", )
        if (!isBleAvailable(result)) return
        var idx = if (index != null) byteArrayOf(index.toByte()) else byteArrayOf()
        L.d("hcia", idx.toString())
        sendCommand(SesameOS3Payload(SesameItemCode.SCRIPT_CURRENT.value, idx), DeviceSegmentType.cipher) { res ->
            L.d("hcia", "result" + res.payload.toList())
            if (res.cmdResultCode == SesameResultCode.success.value) {
                val resp = CHSesamebot2Event.fromByteArray(res.payload)
                if (resp != null) result.invoke(Result.success(CHResultState.CHResultStateBLE(resp))) else result.invoke(Result.failure(
                        NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())
                ))
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }

    override fun getScriptNameList(result: CHResult<CHSesamebot2Status>) {
        L.d("hcia", "[send]getScriptNameList")
        result.invoke(Result.success(CHResultState.CHResultStateBLE(scripts)))
    }

    override fun register(result: CHResult<CHEmpty>) {
        if (deviceStatus != CHDeviceStatus.ReadyToRegister) {
            result.invoke(Result.failure(NSError("Busy", "CBCentralManager", 7)))
            return
        }
        makeApiCall(result) {
            val serverSecret = mSesameToken.toHexString()
            CHAccountManager.jpAPIClient.myDevicesRegisterSesame5Post(deviceId.toString(), CHOS3RegisterReq(productModel.productType().toString(), serverSecret))
            deviceStatus = CHDeviceStatus.Registering
            sendCommand(SesameOS3Payload(SesameItemCode.registration.value, EccKey.getPubK().hexStringToByteArray() + System.currentTimeMillis().toUInt32ByteArray()), DeviceSegmentType.plain) { IRRes ->
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
                    sesame2KeyData = CHDevice(deviceId.toString(), productModel.deviceModel(), null, "0000", ecdhSecretPre16.toHexString(), serverSecret)
                    cipher = SesameOS3BleCipher("customDeviceName", AesCmac(ecdhSecretPre16, 16).computeMac(mSesameToken)!!, ("00" + mSesameToken.toHexString()).hexStringToByteArray())
                    CHDB.CHSS2Model.insert(sesame2KeyData!!) {
                        result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                    }
                    return@sendCommand
                }
                // 新固件统一成 SS5 一样的格式
                // TODO： 重构代码，把 bike2 和 bot2 合并为 CHSesame5Device
                mechStatus = CHSesame5MechStatus(IRRes.payload.toHexString().hexStringToByteArray().sliceArray(0..6))
                // mechSetting = CHSesame5MechSettings(IRRes.payload.toHexString().hexStringToByteArray().sliceArray(7..12))
                val ecdhSecretPre16 = ecdhSecret.sliceArray(0..15)
                val deviceSecret = ecdhSecretPre16.toHexString()
                val candyDevice = CHDevice(deviceId.toString(), advertisement!!.productModel!!.deviceModel(), null, "0000", deviceSecret, serverSecret)
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
        sendCommand(SesameOS3Payload(SesameItemCode.login.value, sessionAuth.sliceArray(0..3)), DeviceSegmentType.plain) { /** 根據設備狀態特殊處理 */ }
    }

    private fun reportBatteryData(payloadString: String) {
        L.d("harry", "[ss5][reportBatteryData]:" + isInternetAvailable() + ", " + !isConnectedByWM2 + ", payload: " + payloadString)
        CHAccountManager.postBatteryData(deviceId.toString().uppercase(), payloadString) {}
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