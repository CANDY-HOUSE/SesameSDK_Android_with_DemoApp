package co.candyhouse.sesame.ble.os3

import android.annotation.SuppressLint
import android.bluetooth.*
import co.candyhouse.sesame.ble.*
import co.candyhouse.sesame.ble.os3.base.CHSesameOS3
import co.candyhouse.sesame.ble.os3.base.SesameOS3BleCipher
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.db.model.createHistagV2
import co.candyhouse.sesame.db.model.hisTagC
import co.candyhouse.sesame.open.*
import co.candyhouse.sesame.open.CHAccountManager.makeApiCall
import co.candyhouse.sesame.open.device.*
import co.candyhouse.sesame.server.CHIotManager
import co.candyhouse.sesame.server.dto.*
import co.candyhouse.sesame.utils.*
import co.candyhouse.sesame.utils.aescmac.AesCmac
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.*

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
                        mechStatus = CHSesameBot2MechStatus(mechShadow.hexStringToByteArray().sliceArray(0..2))
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
        if (deviceStatus.value == CHDeviceLoginStatus.UnLogin && deviceShadowStatus != null) {
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
            CHAccountManager.jpAPIclient.myDevicesRegisterSesame5Post(deviceId.toString(), CHOS3RegisterReq(productModel.productType().toString(), serverSecret))
            deviceStatus = CHDeviceStatus.Registering
            sendCommand(SesameOS3Payload(SesameItemCode.registration.value, EccKey.getPubK().hexStringToByteArray() + System.currentTimeMillis().toUInt32ByteArray()), DeviceSegmentType.plain) { IRRes ->
                /** 根據設備狀態特殊處理 */
                mechStatus = CHSesameBot2MechStatus(IRRes.payload.toHexString().hexStringToByteArray().sliceArray(0..2))
                deviceStatus = if (mechStatus?.isInLockRange == true) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
                val ecdhSecretPre16 = EccKey.ecdh(IRRes.payload.toHexString().hexStringToByteArray().sliceArray(3..66)).sliceArray(0..15)
                sesame2KeyData = CHDevice(deviceId.toString(), productModel.deviceModel(), null, "0000", ecdhSecretPre16.toHexString(), serverSecret)
                cipher = SesameOS3BleCipher("customDeviceName", AesCmac(ecdhSecretPre16, 16).computeMac(mSesameToken)!!, ("00" + mSesameToken.toHexString()).hexStringToByteArray())
                CHDB.CHSS2Model.insert(sesame2KeyData!!) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                }
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

    /** 指令接收 */
    override fun onGattSesamePublish(receivePayload: SSM3PublishPayload) {
        super.onGattSesamePublish(receivePayload)
        if (receivePayload.cmdItCode == SesameItemCode.mechStatus.value) {
            mechStatus = CHSesameBot2MechStatus(receivePayload.payload)
            deviceStatus = if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
        }
    }
}