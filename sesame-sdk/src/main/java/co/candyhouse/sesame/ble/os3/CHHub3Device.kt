package co.candyhouse.sesame.ble.os3

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
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
import co.candyhouse.sesame.open.devices.CHHub3
import co.candyhouse.sesame.open.devices.CHHub3Delegate
import co.candyhouse.sesame.open.devices.CHWifiModule2Delegate
import co.candyhouse.sesame.open.devices.CHWifiModule2MechSettings
import co.candyhouse.sesame.open.devices.CHWifiModule2NetWorkStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceLoginStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceStatus
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.base.NSError
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.CHIotManager
import co.candyhouse.sesame.server.dto.CHOS3RegisterReq
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.utils.EccKey
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame.utils.bytesToShort
import co.candyhouse.sesame.utils.divideArray
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.noHashtoUUID
import co.candyhouse.sesame.utils.toHexString
import co.candyhouse.sesame.utils.toUInt32ByteArray
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.experimental.and

@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
internal class CHHub3Device : CHSesameOS3(), CHHub3, CHDeviceUtil {
    override var mechSetting: CHWifiModule2MechSettings? = CHWifiModule2MechSettings(null, null)
        set(value) {
            if (field != value) {
                field = value
            }
        }

    /** 其他功能: connector */
    override var ssm2KeysMap: MutableMap<String, String> = mutableMapOf()

    /** 設備廣播 */
    override var advertisement: CHadv? = null
        set(value) {
            field = value
            parceADV(value)
        }

    override fun getHub3StatusFromIot(deviceUUID: String, result: CHResult<Byte>) {
        CHAPIClientBiz.getHub3StatusFromIot(deviceUUID) { it ->
            it.onFailure { }
            it.onSuccess {
                val jsonResponse = Gson().toJson(it.data)
                val jsonObject = JSONObject(jsonResponse)

                val eventType = jsonObject.optString("eventType") // 从json中取出 event type
                val isConnectIOT = (eventType == "connected") // 判断是否连接到IOT
                isConnectIOT.apply {
                    mechStatus = CHWifiModule2NetWorkStatus(
                        isConnectIOT,
                        isConnectIOT,
                        isConnectIOT,
                        false,
                        false,
                        false,
                        isConnectIOT == true,
                    )
                }

                // 从json中取出wifi_ssid和wifi_password
                val wifiSSID = jsonObject.optString("wifi_ssid")
                val wifiPassword = jsonObject.optString("wifi_password")
                mechSetting?.wifiSSID = wifiSSID
                mechSetting?.wifiPassWord = wifiPassword
                (delegate as? CHWifiModule2Delegate)?.onAPSettingChanged(this, mechSetting!!)

                val ssks = jsonObject.optString("ssks")
                val ssmSum = ssks?.length?.div(38) ?: 0
                val ssm5KeysMapFromIOT: MutableMap<String, String> = mutableMapOf()
                for (i in 0 until ssmSum) {
                    val ssmID = (ssks?.substring(i * 38, i * 38 + 36))?.lowercase()
                    ssm5KeysMapFromIOT[ssmID.toString()] = "$i"
                }
                if (deviceStatus.value == CHDeviceLoginStatus.unlogined) {
                    ssm2KeysMap.clear()
                    ssm2KeysMap.putAll(ssm5KeysMapFromIOT)
                    (delegate as? CHHub3Delegate)?.onSSM2KeysChanged(this, ssm2KeysMap)
                }
            }
        }
    }

    /** 聯網處理  override fun goIOT() {} */
    override fun goIOT() {
        L.d("hcia", "[hub3]goIOT:")
        getHub3StatusFromIot(deviceId.toString()) { }
        CHIotManager.subscribeHub3(this) { it ->
            it.onSuccess {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val jsonObject = JSONObject(it.data)
                        val state = jsonObject.optJSONObject("state")
                        val reported = state?.optJSONObject("reported")
                        val isConnectIOT = reported?.optBoolean("c")
                        L.d("hcia", "🥝 [hub3]hub3是否連線到IoT:" + isConnectIOT)
                        isConnectIOT?.apply {
                            mechStatus = CHWifiModule2NetWorkStatus(
                                isConnectIOT,
                                isConnectIOT,
                                isConnectIOT,
                                false,
                                false,
                                false,
                                isConnectIOT == true,
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            it.onFailure { }
        }
    }

    /** 指令發送 */
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
                isRegistered = true
                val ecdhSecretPre16 = EccKey.ecdh(IRRes.payload).sliceArray(0..15)
                val wm2Key = ecdhSecretPre16.toHexString()
                val candyDevice = CHDevice(deviceId.toString(), advertisement!!.productModel!!.deviceModel(), null, "0000", wm2Key, serverSecret)
                sesame2KeyData = candyDevice
                deviceStatus = CHDeviceStatus.NoSettings
                val sessionAuth = AesCmac(ecdhSecretPre16, 16).computeMac(mSesameToken)
                cipher = SesameOS3BleCipher("customDeviceName", sessionAuth!!, ("00" + mSesameToken.toHexString()).hexStringToByteArray())
                ssm2KeysMap.clear()
                CHDB.CHSS2Model.insert(candyDevice) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                }
            }
        }
    }

    override fun updateFirmwareBleOnly(onResponse: CHResult<BluetoothDevice>) {
        val result: CHResult<CHEmpty> = { }
        if (!isBleAvailable(result)) {
            onResponse.invoke(Result.failure(NSError("BLE unavailable", "SesameSDK", -2)))
        } else {
            sendCommand(SesameOS3Payload(SesameItemCode.moveTo.value, byteArrayOf())) { res ->
                if (res.cmdResultCode == SesameResultCode.success.value) {
                    onResponse.invoke(Result.success(CHResultState.CHResultStateBLE(advertisement!!.device!!)))
                } else {
                    onResponse.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
                }
            }
        }
    }

    override fun removeSesame(tag: String, result: CHResult<CHEmpty>) {
        if (!isBleAvailable(result)) return
        val noDashUUID = tag?.replace("-", "")
        if (noDashUUID != null) {
            sendCommand(SesameOS3Payload(SesameItemCode.REMOVE_SESAME.value, noDashUUID.hexStringToByteArray())) { ssm2ResponsePayload ->
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
        }
    }

    override fun scanWifiSSID(result: CHResult<CHEmpty>) {
        if (!isBleAvailable(result)) return
        L.d("hcia", "[hub3]請求掃描wifi")
        sendCommand(SesameOS3Payload(SesameItemCode.HUB3_ITEM_CODE_WIFI_SSID.value, byteArrayOf())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun setWifiPassword(password: String, result: CHResult<CHEmpty>) {
        sendCommand(SesameOS3Payload(SesameItemCode.HUB3_ITEM_CODE_WIFI_PASSWORD.value, password.toByteArray())) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                L.d("hcia", "[hub3]設定密碼完畢")
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
        }
    }

    override fun connectWifi(result: CHResult<CHEmpty>) {
        TODO("Not yet implemented")
    }

    override fun insertSesames(sesame: CHDevices, result: CHResult<CHEmpty>) {
        TODO("Not yet implemented")
    }

    override fun setWifiSSID(ssid: String, result: CHResult<CHEmpty>) {
        sendCommand(SesameOS3Payload(SesameItemCode.HUB3_UPDATE_WIFI_SSID.value, ssid.toByteArray())) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                L.d("hcia", "[hub3]設定帳號完畢:" + String(res.payload))
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            } else {
                L.d("hcia", "[hub3]設定wifi錯誤:" + res.cmdResultCode.toString())
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", (res.cmdResultCode).toInt())))
            }
        }
    }

    /** 指令接收 */
    override fun onGattSesamePublish(receivePayload: SSM3PublishPayload) {
        super.onGattSesamePublish(receivePayload)
        L.d("[hub3_say]", "[hub3][收到推送的指令]===>:" + receivePayload.cmdItCode)
        when (receivePayload.cmdItCode) {
            SesameItemCode.mechSetting.value -> {
                if (receivePayload.payload.size < 96) { // 旧的Hub3固件， 只有 60 个字节
                    mechSetting?.wifiSSID =
                        String(receivePayload.payload.copyOfRange(0, 30)).trimEnd(0.toChar(), '?'.toChar()) // 0...29 转换为字符串并去除末尾的零字符和问号字符
                    mechSetting?.wifiPassWord =
                        String(receivePayload.payload.copyOfRange(30, 60)).trimEnd(0.toChar(), '?'.toChar()) // 30...59 转换为字符串并去除末尾的零字符和问号字符
                } else { // 新的 Hub3 固件， 有96个字节
                    mechSetting?.wifiSSID =
                        String(receivePayload.payload.copyOfRange(0, 32)).trimEnd(0.toChar(), '?'.toChar()) // 0...31 转换为字符串并去除末尾的零字符和问号字符
                    mechSetting?.wifiPassWord =
                        String(receivePayload.payload.copyOfRange(32, 96)).trimEnd(0.toChar(), '?'.toChar()) // 32...95 转换为字符串并去除末尾的零字符和问号字符
                }
                (delegate as? CHWifiModule2Delegate)?.onAPSettingChanged(this, mechSetting!!)
            }

            SesameItemCode.mechStatus.value -> {
                L.d("hcia", "[hub3] 收到mechStatus:" + (receivePayload.payload.toHexString()))
                val isAp: Boolean = (receivePayload.payload[0] and 2) > 0
                val isNEt: Boolean = (receivePayload.payload[0] and 4) > 0
                val isIot: Boolean = (receivePayload.payload[0] and 8) > 0
                val isAPCheck: Boolean = (receivePayload.payload[0] and 16) > 0
                val isAPConnecting: Boolean = (receivePayload.payload[0] and 32) > 0
                val isNETConnecting: Boolean = (receivePayload.payload[0] and 64) > 0
                val isIOTConnecting: Boolean = receivePayload.payload[0] < 0
                mechStatus = CHWifiModule2NetWorkStatus(isAp, isNEt, isIot, isAPConnecting, isNETConnecting, isIOTConnecting, isAPCheck)
            }

            SesameItemCode.PUB_KEY_SESAME.value -> {
                L.d("hcia", "[1][hub3][收到推送的ssm列表]===>:" + receivePayload.payload.toHexString())
                ssm2KeysMap.clear()
                val keyDatas = receivePayload.payload.divideArray(23)
                keyDatas.forEachIndexed { index, it ->
                    val lock_status = it[22].toInt()
                    if (lock_status != 0) {
                        val ss5_id = it.sliceArray(IntRange(0, 15))
                        val ssmID = ss5_id.toHexString().noHashtoUUID().toString()
                        L.d("hcia", "[2][hub3][走訪列表並存下ssm]" + ssmID) // 1120031c-0903-0219-9c00-1400ffffffff
                        ssm2KeysMap.put(ssmID, "$index")
                    }
                }
                L.d("hcia", "[3][hub3][存下的ssm清單]" + ssm2KeysMap) //{1120031c-0903-0219-9c00-1400ffffffff=[B@5e481ae}
                (delegate as? CHHub3Delegate)?.onSSM2KeysChanged(this, ssm2KeysMap)
            }

            SesameItemCode.moveTo.value -> {
                (delegate as? CHHub3Delegate)?.onOTAProgress(this, receivePayload.payload.first())
            }

            SesameItemCode.HUB3_ITEM_CODE_SSID_FIRST.value -> {}
            SesameItemCode.HUB3_ITEM_CODE_SSID_LAST.value -> {}
            SesameItemCode.HUB3_ITEM_CODE_SSID_NOTIFY.value -> {
                val ssidRssi = bytesToShort(receivePayload.payload[0], receivePayload.payload[1])
                val ssidStr = String(receivePayload.payload.drop(2).toByteArray())
                (delegate as? CHWifiModule2Delegate)?.onScanWifiSID(this, ssidStr, ssidRssi)
            }

            else -> {
                L.d(
                    "hcia", msg = "!![hub3][pub][${receivePayload.cmdItCode}]"
                )
            }
        }
    }

    override fun <T> isBleAvailable(result: CHResult<T>): Boolean {
        return (this as CHDevices).isBleAvailable(result)
    }

    override fun toggle(historytag: ByteArray?, result: CHResult<CHEmpty>) {
        CHAPIClientBiz.updateHub3Switch(historytag, this, result)
    }

}