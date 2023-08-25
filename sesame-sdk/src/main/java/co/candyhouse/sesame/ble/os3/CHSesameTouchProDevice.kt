package co.candyhouse.sesame.ble.os3

import android.annotation.SuppressLint
import android.bluetooth.*
import co.candyhouse.sesame.ble.*
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.checkBle
import co.candyhouse.sesame.ble.os2.CHError
import co.candyhouse.sesame.ble.os3.base.CHSesameOS3
import co.candyhouse.sesame.ble.os3.base.SesameOS3BleCipher
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.open.*
import co.candyhouse.sesame.open.CHAccountManager.makeApiCall
import co.candyhouse.sesame.open.device.*
import co.candyhouse.sesame.server.dto.*
import co.candyhouse.sesame.utils.*
import co.candyhouse.sesame.utils.EccKey
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.aescmac.AesCmac
import java.util.*

class CHSesameTouchCard(data: ByteArray) {
    val cardType = data[0]
    val idLength = data[1]
    val cardID = data.sliceArray(2..idLength + 1).toHexString()
    val nameIndex = idLength + 2
    val nameLength = data[nameIndex]
    val cardName = String(data.sliceArray(nameIndex + 1..nameIndex + nameLength))
}

@SuppressLint("MissingPermission") internal class CHSesameTouchProDevice : CHSesameOS3(), CHSesameTouchPro, CHDeviceUtil {

    /** 其他功能: connector */
    override var ssm2KeysMap: MutableMap<String, ByteArray> = mutableMapOf()

    /** 設備廣播 */
    override var advertisement: CHadv? = null
        set(value) {
            field = value
            parceADV(value)
            /** 保留廣播通訊 */
            value?.let {
                /** 保留廣播通訊 */
//                isHistory = it.adv_tag_b1
            }
        }

    /** 聯網處理  override fun goIOT() {} */

    /** 指令發送 */
    override fun keyBoardPassCodeModeGet(result: CHResult<Byte>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_PASSCODE_MODE_GET.value, byteArrayOf())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(res.payload[0])))
        }
    }

    override fun keyBoardPassCodeModeSet(mode: Byte, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_PASSCODE_MODE_SET.value, byteArrayOf(mode))) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun keyBoardPassCodeDelete(ID: String, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_PASSCODE_DELETE.value, ID.hexStringToByteArray())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun keyBoardPassCode(result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_PASSCODE_GET.value, byteArrayOf())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun keyBoardPassCodeChange(ID: String, name: String, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_PASSCODE_CHANGE.value, byteArrayOf(ID.hexStringToByteArray().size.toByte()) + ID.hexStringToByteArray() + name.toByteArray())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun fingerPrintModeGet(result: CHResult<Byte>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_FINGERPRINT_MODE_GET.value, byteArrayOf())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(res.payload[0])))
        }
    }

    override fun fingerPrintModeSet(mode: Byte, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_FINGERPRINT_MODE_SET.value, byteArrayOf(mode))) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun fingerPrintDelete(ID: String, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_FINGERPRINT_DELETE.value, ID.hexStringToByteArray())) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun fingerPrints(result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_FINGERPRINT_GET.value, byteArrayOf())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun fingerPrintsChange(ID: String, name: String, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_FINGERPRINT_CHANGE.value, byteArrayOf(ID.hexStringToByteArray().size.toByte()) + ID.hexStringToByteArray() + name.toByteArray())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun cardModeGet(result: CHResult<Byte>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_CARD_MODE_GET.value, byteArrayOf())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(res.payload[0])))
        }
    }

    override fun cardModeSet(mode: Byte, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_CARD_MODE_SET.value, byteArrayOf(mode))) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun cardDelete(ID: String, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_CARD_DELETE.value, ID.hexStringToByteArray())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun cardChange(ID: String, name: String, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_CARD_CHANGE.value, byteArrayOf(ID.hexStringToByteArray().size.toByte()) + ID.hexStringToByteArray() + name.toByteArray())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun cards(result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_CARD_GET.value, byteArrayOf())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun login(token: String?) {
        deviceStatus = CHDeviceStatus.BleLogining
        val sessionAuth = AesCmac(sesame2KeyData!!.secretKey.hexStringToByteArray(), 16).computeMac(mSesameToken)
        cipher = SesameOS3BleCipher("customDeviceName", sessionAuth!!, ("00" + mSesameToken.toHexString()).hexStringToByteArray())
        val loginTag = AesCmac(sesame2KeyData!!.secretKey.hexStringToByteArray(), 16).computeMac(mSesameToken)
        sendCommand(SesameOS3Payload(SesameItemCode.login.value, loginTag!!), DeviceSegmentType.plain) { res ->
            deviceStatus = CHDeviceStatus.Unlocked
            L.d("hcia", "[touchpro][login]][ok]:")
        }
    }

    override fun register(result: CHResult<CHEmpty>) {
        if (deviceStatus != CHDeviceStatus.ReadyToRegister) {
            result.invoke(Result.failure(NSError("Busy", "CBCentralManager", 7)))
            return
        }
        deviceStatus = CHDeviceStatus.Registering
        makeApiCall(result) {
            val serverSecret = mSesameToken.toHexString()
            deviceStatus = CHDeviceStatus.Registering
            sendCommand(SesameOS3Payload(SesameItemCode.registration.value, EccKey.getPubK().hexStringToByteArray()), DeviceSegmentType.plain) { IRRes ->
                isRegistered = true
                val ecdhSecretPre16 = EccKey.ecdh(IRRes.payload).sliceArray(0..15)
                val wm2Key = ecdhSecretPre16.toHexString()
//                val candyDevice = CHDevice(deviceId.toString(), productModel.deviceModel(), null, "", wm2Key, "")
                val candyDevice = CHDevice(deviceId.toString(), advertisement!!.productModel!!.deviceModel(), null, "0000", wm2Key, serverSecret)
                sesame2KeyData = candyDevice
                deviceStatus = CHDeviceStatus.Unlocked
                val sessionAuth = AesCmac(ecdhSecretPre16, 16).computeMac(mSesameToken)
                cipher = SesameOS3BleCipher("customDeviceName", sessionAuth!!, ("00" + mSesameToken.toHexString()).hexStringToByteArray())
                ssm2KeysMap.clear()
                CHDB.CHSS2Model.insert(candyDevice) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                }
                sendCommand(SesameOS3Payload(SesameItemCode.mechStatus.value, byteArrayOf()), DeviceSegmentType.cipher) { res ->
                    mechStatus = CHSesameTouchProMechStatus(res.payload)
                    L.d("harry", "[reg]getBatteryPrecentage" + mechStatus?.getBatteryPrecentage())
                    L.d("harry", "[reg]getBatteryVoltage" + mechStatus?.getBatteryVoltage())
                }
            }
        }
    }

    override fun insertSesame(sesame: CHDevices, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        if (sesame !is CHSesameLock) {// 不是锁不处理
            L.d("hcia", "[SSM BTN]isLocker?")
            result.invoke(Result.failure(CHError.BleInvalidAction.value))
            return
        }
        L.d("hcia", "送出鑰匙sesame.getKey():" + sesame.getKey())

        if (sesame is CHSesameOS3) {///ss5/5pro,bike2
            val ssm = sesame as CHDeviceUtil
            val noDashUUID = ssm.sesame2KeyData!!.deviceUUID.replace("-", "")
            val noDashUUIDDATA = noDashUUID.hexStringToByteArray()
            val ssmSecKa = ssm.sesame2KeyData!!.secretKey.hexStringToByteArray()
            sendCommand(SesameOS3Payload(SesameItemCode.ADD_SESAME.value, noDashUUIDDATA + ssmSecKa)) { ssm2ResponsePayload ->
//                L.d("hcia", "ADD_SESAME cmdResultCode:" + ssm2ResponsePayload.cmdResultCode)
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
        } else {/// ss3/4,bot1,bike1
            val ssm = sesame as CHDeviceUtil
            val noDashUUID = ssm.sesame2KeyData!!.deviceUUID.replace("-", "")
            val b64k = noDashUUID.hexStringToByteArray().base64Encode().replace("=", "")
            val ssmIRData = b64k.toByteArray()
            val ssmPKData = ssm.sesame2KeyData!!.sesame2PublicKey.hexStringToByteArray()
            val ssmSecKa = ssm.sesame2KeyData!!.secretKey.hexStringToByteArray()
            val allKey = ssmIRData + ssmPKData + ssmSecKa
            sendCommand(SesameOS3Payload(SesameItemCode.ADD_SESAME.value, allKey)) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
        }

    }

    override fun removeSesame(tag: String, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        if (ssm2KeysMap.get(tag)!!.get(0).toInt() == 0x04) {// ss4
            val noDashUUID = tag.replace("-", "")
            val b64k = noDashUUID.hexStringToByteArray().base64Encode().replace("=", "")
            val ssmIRData = b64k.toByteArray()
            sendCommand(SesameOS3Payload(SesameItemCode.REMOVE_SESAME.value, ssmIRData)) { ssm2ResponsePayload ->
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
        } else {//ss5
            val noDashUUID = tag.replace("-", "")
            sendCommand(SesameOS3Payload(SesameItemCode.REMOVE_SESAME.value, noDashUUID.hexStringToByteArray())) { ssm2ResponsePayload ->
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
        }
    }

    /** 指令接收 */

    override fun onGattSesamePublish(receivePayload: SSM3PublishPayload) {
        super.onGattSesamePublish(receivePayload)

        if (receivePayload.cmdItCode == SesameItemCode.mechStatus.value) {
            mechStatus = CHSesameTouchProMechStatus(receivePayload.payload)
            L.d("hcia", "getBatteryPrecentage" + mechStatus?.getBatteryPrecentage())
            L.d("hcia", "getBatteryVoltage" + mechStatus?.getBatteryVoltage())
        }
        if (receivePayload.cmdItCode == SesameItemCode.SSM_OS3_FINGERPRINT_CHANGE.value) {
            val card = CHSesameTouchCard(receivePayload.payload)
            (delegate as? CHSesameTouchProDelegate)?.onFingerPrintChanged(this, card.cardID, card.cardName, card.cardType)
        } else if (receivePayload.cmdItCode == SesameItemCode.SSM_OS3_FINGERPRINT_FIRST.value) {
            (delegate as? CHSesameTouchProDelegate)?.onFingerPrintReceiveStart(this)
        } else if (receivePayload.cmdItCode == SesameItemCode.SSM_OS3_FINGERPRINT_LAST.value) {
            (delegate as? CHSesameTouchProDelegate)?.onFingerPrintReceiveEnd(this)
        } else if (receivePayload.cmdItCode == SesameItemCode.SSM_OS3_FINGERPRINT_NOTIFY.value) {
            val card = CHSesameTouchCard(receivePayload.payload)
            (delegate as? CHSesameTouchProDelegate)?.onFingerPrintReceive(this, card.cardID, card.cardName, card.cardType)
        }
        if (receivePayload.cmdItCode == SesameItemCode.SSM_OS3_CARD_CHANGE.value) {
            val card = CHSesameTouchCard(receivePayload.payload)
            (delegate as? CHSesameTouchProDelegate)?.onCardChanged(this, card.cardID, card.cardName, card.cardType)
        } else if (receivePayload.cmdItCode == SesameItemCode.SSM_OS3_CARD_NOTIFY.value) {
            val card = CHSesameTouchCard(receivePayload.payload)
            (delegate as? CHSesameTouchProDelegate)?.onCardReceive(this, card.cardID, card.cardName, card.cardType)
        } else if (receivePayload.cmdItCode == SesameItemCode.SSM_OS3_CARD_FIRST.value) {
            (delegate as? CHSesameTouchProDelegate)?.onCardReceiveStart(this)
        } else if (receivePayload.cmdItCode == SesameItemCode.SSM_OS3_CARD_LAST.value) {
            (delegate as? CHSesameTouchProDelegate)?.onCardReceiveEnd(this)
        }
        if (receivePayload.cmdItCode == SesameItemCode.SSM_OS3_PASSCODE_CHANGE.value) {
            val card = CHSesameTouchCard(receivePayload.payload)
            (delegate as? CHSesameTouchProDelegate)?.onKeyBoardChanged(this, card.cardID, card.cardName, card.cardType)
        } else if (receivePayload.cmdItCode == SesameItemCode.SSM_OS3_PASSCODE_NOTIFY.value) {
            val card = CHSesameTouchCard(receivePayload.payload)
            (delegate as? CHSesameTouchProDelegate)?.onKeyBoardReceive(this, card.cardID, card.cardName, card.cardType)
        } else if (receivePayload.cmdItCode == SesameItemCode.SSM_OS3_PASSCODE_FIRST.value) {
            (delegate as? CHSesameTouchProDelegate)?.onKeyBoardReceiveStart(this)
        } else if (receivePayload.cmdItCode == SesameItemCode.SSM_OS3_PASSCODE_LAST.value) {
            (delegate as? CHSesameTouchProDelegate)?.onKeyBoardReceiveEnd(this)
        }
        if (receivePayload.cmdItCode == SesameItemCode.PUB_KEY_SESAME.value) {
//            L.d("hcia", "[ds][PUB][KEY]===>:" + receivePayload.payload.toHexString())
            ssm2KeysMap.clear()
            val keyDatas = receivePayload.payload.divideArray(23)
            keyDatas.forEach {
                val lock_status = it[22].toInt()
//                L.d("hcia", "lock_status:" + lock_status)
                if (lock_status != 0) {
//                    L.d("hcia", "it[21].toInt():" + it[21].toInt())
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
            L.d("hcia", "[TPO][ssm2KeysMap]" + ssm2KeysMap)
            (delegate as? CHSesameTouchProDelegate)?.onSSM2KeysChanged(this, ssm2KeysMap)
        }
    }

}
