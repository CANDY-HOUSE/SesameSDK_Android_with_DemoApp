package co.candyhouse.sesame.ble.os3

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.CHadv
import co.candyhouse.sesame.ble.DeviceSegmentType
import co.candyhouse.sesame.ble.Hub3ItemCode
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
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.open.device.CHHub3Delegate
import co.candyhouse.sesame.open.device.CHWifiModule2Delegate
import co.candyhouse.sesame.open.device.CHWifiModule2MechSettings
import co.candyhouse.sesame.open.device.CHWifiModule2NetWorkStatus
import co.candyhouse.sesame.open.device.MatterProductModel
import co.candyhouse.sesame.open.device.NSError
import co.candyhouse.sesame.server.CHIotManager
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.server.dto.CHOS3RegisterReq
import co.candyhouse.sesame.utils.EccKey
import co.candyhouse.sesame.utils.HexLog
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame.utils.bytesToShort
import co.candyhouse.sesame.utils.divideArray
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.noHashtoUUID
import co.candyhouse.sesame.utils.toHexString
import co.candyhouse.sesame.utils.toUInt32ByteArray
import co.candyhouse.sesame2.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.experimental.and

class CHHub3IRCode(data: ByteArray) {
    val irCodeID = data[0].toString()
    private val nameLength = data[1]
    val irCodeName = String(data.sliceArray(2 + 1..2 + nameLength))
}

@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
internal class CHHub3Device : CHSesameOS3(), CHHub3, CHDeviceUtil {
    override var versionTagFromIoT: String? = null
    override var hub3LastFirmwareVer: String? = null
    override var hub3Brightness: Byte = (255).toByte()
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
            /** 保留廣播通訊 */
            value?.let {
                /** 保留廣播通訊 */
//                isHistory = it.adv_tag_b1
            }
        }

    override fun getVersionTag(result: CHResult<String>) {
        L.d("harry", "getVersionTag")
        if (!(this as CHDevices).isBleAvailable(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.versionTag.value, byteArrayOf()), DeviceSegmentType.cipher) { res ->
            L.d("harry", "getVersionTag res: " + String(res.payload))
            result.invoke(Result.success(CHResultState.CHResultStateBLE("B:${String(res.payload)}")))
        }
        CHAccountManager.getVersion {
            it.onSuccess {
                try {
                    val jsonResponse = Gson().toJson(it.data)
                    val jsonObject = JSONObject(jsonResponse)
                    val hub3Value = jsonObject.optJSONObject("data")?.optString("hub3")
                    hub3Value?.apply {
                        result.invoke(Result.success(CHResultState.CHResultStateNetworks("N:${hub3Value}")))
                    }
                } catch (_: JsonSyntaxException) {
                }
            }
            it.onFailure {}
        }
    }

    private fun getOtaProgress() {
        L.d("harry", "[hub3] getOtaProgress:")
        val topic = "hub3/ota/${deviceId.toString().uppercase()}/progress" // hub3/ota/00000000-055A-FD81-0D00-D432048D8781/progress
        L.d("harry", "[hub3] getOtaProgress 訂閱主題:$topic")
        CHIotManager.subscribeTopic(topic) { it ->
            it.onSuccess {
                L.d("hub3", "data: " + it.data.HexLog())
                (delegate as? CHHub3Delegate)?.onOTAProgress(this, it.data.first())
            }
        }
    }

    override fun updateHub3Firmware(deviceUUID: String, result: CHResult<CHEmpty>) {
        L.d("harry", "[updateHub3Firmware] [deviceUUID: $deviceUUID]")
        CHAccountManager.updateHub3Firmware(deviceUUID) { it ->
            it.onFailure { }
            it.onSuccess {
                L.d("harry", "data: " + it.data.toString())
            }
        }
        getOtaProgress()
        // 对齐iOS，HUB3固件升级默认不显示0%
        //(delegate as? CHHub3Delegate)?.onOTAProgress(this, 0)
    }

    override fun setHub3Brightness(brightness: Byte, result: CHResult<Byte>) {
        L.d("harry", "[setHub3Brightness] [brightness: ${brightness.toUByte()}]")
        if (!isBleAvailable(result)) return
        sendCommand(SesameOS3Payload(Hub3ItemCode.HUB3_ITEM_CODE_LED_DUTY.value, byteArrayOf(brightness)), DeviceSegmentType.cipher) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(brightness)))
        }
    }

    override fun getHub3StatusFromIot(deviceUUID: String, result: CHResult<Byte>) {
        L.d("harry", "[getHub3StatusFromIot] [deviceUUID: $deviceUUID]")
        CHAccountManager.getHub3StatusFromIot(deviceUUID) { it ->
            it.onFailure { }
            it.onSuccess {
                // {ts=1.735960612028E12, wifi_password=55667788, v=3.0-13-dca4da, deviceUUID=00000000-055A-FD81-0D00-D432048D8781, timestamp=1.735959722063E12, wifi_ssid=TanguoVPN, eventType=connected, ssks=42503131-3130-380E-005B-3A255606017802}
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
                L.d("harry", "wifiSSID: $wifiSSID, wifiPassword: $wifiPassword")

                mechSetting?.wifiSSID = wifiSSID
                mechSetting?.wifiPassWord = wifiPassword
                // 从json中取出固件版本号 v
                versionTagFromIoT = jsonObject.optString("v")
                hub3LastFirmwareVer = jsonObject.optString("hub3LastFirmwareVer")
                L.d("harry", "versionTagFromIoT: $versionTagFromIoT")
                (delegate as? CHWifiModule2Delegate)?.onAPSettingChanged(this, mechSetting!!)

                val ssks = jsonObject.optString("ssks")
                L.d("hcia", "🥝 [hub3]数据库里存的ssm列表:$ssks, len: " + ssks?.length) // 42503131-3130-380E-005B-3A255606017802
                val ssmSum = ssks?.length?.div(38) ?: 0
                val ssm5KeysMapFromIOT: MutableMap<String, String> = mutableMapOf()
                for (i in 0 until ssmSum) {
                    val ssmID = (ssks?.substring(i * 38, i * 38 + 36))?.lowercase()
                    L.d("hcia", "[hub3]数据库里存的ssm$i: $ssmID")
                    ssm5KeysMapFromIOT[ssmID.toString()] = "$i"
                }
                if (deviceStatus.value == CHDeviceLoginStatus.UnLogin) {
                    L.d("hcia", "[hub3][-]hub3手機登入狀態:" + deviceStatus.value)
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
                        L.d("harry", "data: " + it.data)
                        // {"state":{"reported":{"c":false,"v":""}},"metadata":{"reported":{"c":{"timestamp":1736395348},"v":{"timestamp":1736395348}}},"version":18493,"timestamp":1736395348}
                        val jsonObject = JSONObject(it.data)
                        L.d("harry", "jsonObject: $jsonObject")
                        val state = jsonObject.optJSONObject("state")
                        L.d("harry", "state: $state")
                        val reported = state?.optJSONObject("reported")
                        L.d("harry", "reported: $reported")
                        val isConnectIOT = reported?.optBoolean("c")
                        L.d("hcia", "🥝 [hub3]hub3是否連線到IoT:" + isConnectIOT)
                        versionTagFromIoT = reported?.optString("v")
                        L.d("harry", "versionTagFromIoT: $versionTagFromIoT")
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
            it.onFailure { L.d("harry", "subscribeHub3 fail!!!") }
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
//            L.d("hcia", "[hub3][login]][ok]:")
        }
    }

    override fun register(result: CHResult<CHEmpty>) {
        L.d("harry", "hub3 register")
        if (deviceStatus != CHDeviceStatus.ReadyToRegister) {
            result.invoke(Result.failure(NSError("Busy", "CBCentralManager", 7)))
            return
        }
        deviceStatus = CHDeviceStatus.Registering
        makeApiCall(result) {
            val serverSecret = mSesameToken.toHexString()
            CHAccountManager.jpAPIclient.myDevicesRegisterSesame5Post(
                deviceId.toString(), CHOS3RegisterReq(productModel.productType().toString(), serverSecret)
            )// todo 不需要server 認證註解此行
            deviceStatus = CHDeviceStatus.Registering
            sendCommand(SesameOS3Payload(SesameItemCode.registration.value, EccKey.getPubK().hexStringToByteArray() + System.currentTimeMillis().toUInt32ByteArray()), DeviceSegmentType.plain) { IRRes ->
                isRegistered = true
                L.d("harry", "[hub3][reg][IRRes]:" + IRRes.payload.toHexString())
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

    private fun deleteSesameShadow(sesame: CHDevices) {
        if (BuildConfig.DEBUG) { // 如果APP是DEBUG版本
            CHIotManager.deleteThingShadow(sesame) // 清除掉影子
            return
        }
    }

    /** update Hub3 firmware */
    override fun updateFirmware(onResponse: CHResult<BluetoothDevice>) {
        L.d("harry", "【Hub3】【updateFirmware】")
        var result: CHResult<CHEmpty> = { }
        if (!isBleAvailable(result)) {
            L.d("sf", "OTA通道升级固件...")
            // 如果藍牙不可用，通过 WebSocket 转 IoT 更新固件。 不要直接走IoT， 为以后全部切换到 WebSocket 做准备。
            updateHub3Firmware(deviceId.toString(), result)
        } else {
            // 蓝牙通道升级固件
            L.d("sf", "蓝牙通道升级固件...")
            sendCommand(SesameOS3Payload(SesameItemCode.moveTo.value, byteArrayOf())) { res ->
                if (res.cmdResultCode == SesameResultCode.success.value) {
                    onResponse.invoke(Result.success(CHResultState.CHResultStateBLE(advertisement!!.device!!)))
                } else {
                    onResponse.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
                }
            }
        }

    }

    override fun insertSesame(sesame: CHDevices, nickName: String, matterProductModel: MatterProductModel, result: CHResult<CHEmpty>) {
        if (!isBleAvailable(result)) return
        deleteSesameShadow(sesame)
        val sesameNameByteArrayLength: Byte = nickName.toByteArray().size.toByte()
        L.d("hub3", "name: $nickName; sesameNameByteArrayLength: $sesameNameByteArrayLength; hex: " + nickName.toByteArray().toHexString())
        val ssm = sesame as CHDeviceUtil
        val noDashUUID = ssm.sesame2KeyData!!.deviceUUID.replace("-", "")
        val noDashUUIDDATA = noDashUUID.hexStringToByteArray() // 16 bytes
        val ssmSecKa = ssm.sesame2KeyData!!.secretKey.hexStringToByteArray() // 16 bytes
        L.d("hub3", "productType:" + byteArrayOf(sesame.productModel.productType().toByte()).toHexString())

        sendCommand(
            SesameOS3Payload(
                SesameItemCode.ADD_SESAME.value, noDashUUIDDATA + ssmSecKa + byteArrayOf(sesameNameByteArrayLength) + nickName.toByteArray() + byteArrayOf(sesame.productModel.productType().toByte()) + byteArrayOf(
                    matterProductModel.value.toByte()
                )
            )
        ) { ssmResponsePayload ->
            L.d("hcia", "ADD_SESAME cmdResultCode:" + ssmResponsePayload.cmdResultCode)
            L.d("harry", "matterProductModel.value: " + matterProductModel.value.toString())
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
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

    override fun irModeSet(mode: Byte, result: CHResult<CHEmpty>) {
        if (!isBleAvailable(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_IR_MODE_SET.value, byteArrayOf(mode))) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun irModeGet(result: CHResult<Byte>) {
        L.d("hcia", "[送]irModeGet")
        if (!isBleAvailable(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_IR_MODE_GET.value, byteArrayOf())) { res ->
            L.d("hcia", "[收]irModeGet: " + res.payload[0])
            result.invoke(Result.success(CHResultState.CHResultStateBLE(res.payload[0])))
        }
    }

    override fun irCodeEmit(id: String, result: CHResult<CHEmpty>) {
        L.d("harry", "irCodeEmit: $id")
        if (!isBleAvailable(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_IR_CODE_EMIT.value, byteArrayOf(id.hexStringToByteArray()[0]))) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun irCodeEmit(topic: String, data: ByteArray, result: CHResult<CHEmpty>) {
        if (!isBleAvailable(result)) return
        CHIotManager.publishData(topic, data)
    }


    override fun irCodeDelete(topic: String, data: ByteArray) {
//        if (checkBle(result)) return
//        CHIotManager.publishData(topic, data)
    }

    override fun irCodeDelete(id: String, result: CHResult<CHEmpty>) {
        if (!isBleAvailable(result)) return
        L.d("hcia", "[送]irCodeDelete: $id")
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_IR_CODE_DELETE.value, byteArrayOf(id.hexStringToByteArray()[0]))) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun irCodeChange(id: String, name: String, result: CHResult<CHEmpty>) {
        if (!isBleAvailable(result)) return
        L.d("hcia", "[送]irCodeChange: $id name: $name")
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_IR_CODE_CHANGE.value, byteArrayOf(id.hexStringToByteArray()[0]) + name.toByteArray())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun getMatterPairingCode(result: CHResult<ByteArray>) {
        if (!isBleAvailable(result)) return
        L.d("harry", "[发]getMatterPairingCode")
        sendCommand(SesameOS3Payload(SesameItemCode.HUB3_MATTER_PAIRING_CODE.value, byteArrayOf())) { res ->
            L.d("harry", "[收]getMatterPairingCode: " + res.payload.toHexString())
            result.invoke(Result.success(CHResultState.CHResultStateBLE(res.payload)))
        }
    }

    override fun openMatterPairingWindow(result: CHResult<Byte>) {
        if (!isBleAvailable(result)) return
        L.d("harry", "[发] openMatterPairingWindow")
        sendCommand(SesameOS3Payload(SesameItemCode.HUB3_MATTER_PAIRING_WINDOW.value, byteArrayOf())) { res ->
            L.d("harry", "[收] openMatterPairingWindow: " + res.payload.toHexString())
            result.invoke(Result.success(CHResultState.CHResultStateBLE(res.payload[0])))
        }
    }

    override fun getIRCodes(result: CHResult<CHEmpty>) {
        if (!isBleAvailable(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_IR_CODE_GET.value, byteArrayOf())) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "GetIRCodes", res.cmdResultCode.toInt())))
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
                    mechSetting?.wifiSSID = String(receivePayload.payload.copyOfRange(0, 30)).trimEnd(0.toChar(), '?'.toChar()) // 0...29 转换为字符串并去除末尾的零字符和问号字符
                    mechSetting?.wifiPassWord = String(receivePayload.payload.copyOfRange(30, 60)).trimEnd(0.toChar(), '?'.toChar()) // 30...59 转换为字符串并去除末尾的零字符和问号字符
                } else { // 新的 Hub3 固件， 有96个字节
                    mechSetting?.wifiSSID = String(receivePayload.payload.copyOfRange(0, 32)).trimEnd(0.toChar(), '?'.toChar()) // 0...31 转换为字符串并去除末尾的零字符和问号字符
                    mechSetting?.wifiPassWord = String(receivePayload.payload.copyOfRange(32, 96)).trimEnd(0.toChar(), '?'.toChar()) // 32...95 转换为字符串并去除末尾的零字符和问号字符
                }
                (delegate as? CHWifiModule2Delegate)?.onAPSettingChanged(this, mechSetting!!)
                multicastDelegate.invokeAll {
                    (this as? CHHub3Delegate)?.onAPSettingChanged(this@CHHub3Device, mechSetting!!)
                }
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
                multicastDelegate.invokeAll {
                    (this as? CHHub3Delegate)?.onSSM2KeysChanged(this@CHHub3Device, ssm2KeysMap)
                }
            }

            SesameItemCode.moveTo.value -> {
                L.d("harry", "moveTo:" + receivePayload.payload[0])
                (delegate as? CHHub3Delegate)?.onOTAProgress(this, receivePayload.payload.first())
                multicastDelegate.invokeAll {
                    (this as? CHHub3Delegate)?.onOTAProgress(this@CHHub3Device, receivePayload.payload.first())
                }
            }

            SesameItemCode.HUB3_ITEM_CODE_SSID_FIRST.value -> {}
            SesameItemCode.HUB3_ITEM_CODE_SSID_LAST.value -> {}
            SesameItemCode.HUB3_ITEM_CODE_SSID_NOTIFY.value -> {
                val ssidRssi = bytesToShort(receivePayload.payload[0], receivePayload.payload[1])
                val ssidStr = String(receivePayload.payload.drop(2).toByteArray())
                (delegate as? CHWifiModule2Delegate)?.onScanWifiSID(this, ssidStr, ssidRssi)
                multicastDelegate.invokeAll {
                    (this as? CHWifiModule2Delegate)?.onScanWifiSID(this@CHHub3Device, ssidStr, ssidRssi)
                }
            }

            SesameItemCode.SSM_OS3_IR_CODE_FIRST.value -> {
                (delegate as? CHHub3Delegate)?.onIRCodeReceiveStart(this)
                multicastDelegate.invokeAll {
                    (this as? CHHub3Delegate)?.onIRCodeReceiveStart(this@CHHub3Device)
                }
                L.d("[hub3_say_ir]", "[SSM_OS3_IR_CODE_FIRST]")
            }

            SesameItemCode.SSM_OS3_IR_CODE_LAST.value -> {
                (delegate as? CHHub3Delegate)?.onIRCodeReceiveEnd(this)
                multicastDelegate.invokeAll {
                    (this as? CHHub3Delegate)?.onIRCodeReceiveEnd(this@CHHub3Device)
                }
                L.d("[hub3_say_ir]", "[SSM_OS3_IR_CODE_LAST]")
            }

            SesameItemCode.SSM_OS3_IR_CODE_NOTIFY.value -> {
                L.d("[hub3_say_ir]", "[SSM_OS3_IR_CODE_NOTIFY]: " + receivePayload.payload.toHexString())
                (delegate as? CHHub3Delegate)?.onIRCodeReceive(
                    this, String.format("%02X", receivePayload.payload[0]), if (receivePayload.payload.size > 2) {
                        String(receivePayload.payload.copyOfRange(2, receivePayload.payload.size))
                    } else {
                        ""
                    }
                )
                multicastDelegate.invokeAll {
                    L.d("[hub3_say_ir]", "[SSM_OS3_IR_CODE_NOTIFY]: " + receivePayload.payload.toHexString() + "  " + this.hashCode())
                    (this as? CHHub3Delegate)?.onIRCodeReceive(
                        this@CHHub3Device,
                        String.format("%02X", receivePayload.payload[0]),
                        if (receivePayload.payload.size > 2) {
                            String(
                                receivePayload.payload.copyOfRange(
                                    2,
                                    receivePayload.payload.size
                                )
                            )
                        } else {
                            ""
                        }
                    )
                }
            }

            SesameItemCode.SSM_OS3_IR_CODE_CHANGE.value -> {
                L.d("[hub3_say_ir]", "[SSM_OS3_IR_CODE_CHANGE]: " + receivePayload.payload.toHexString())
                (delegate as? CHHub3Delegate)?.onIRCodeChanged(
                    this, String.format("%02X", receivePayload.payload[0]), if (receivePayload.payload.size > 2) {
                        String(receivePayload.payload.copyOfRange(2, receivePayload.payload.size))
                    } else {
                        ""
                    }
                )
                multicastDelegate.invokeAll {
                    (this as? CHHub3Delegate)?.onIRCodeChanged(
                        this@CHHub3Device, String.format("%02X", receivePayload.payload[0]), if (receivePayload.payload.size > 2) {
                            String(receivePayload.payload.copyOfRange(2, receivePayload.payload.size))
                        } else {
                            ""
                        }
                    )
                }

            }

            SesameItemCode.SSM_OS3_IR_MODE_GET.value -> {
                L.d("hcia", "[收]SSM_OS3_IR_MODE_GET:" + receivePayload.payload[0])
                multicastDelegate.invokeAll {
                    (this as? CHHub3Delegate)?.onIRModeReceive(this@CHHub3Device, receivePayload.payload[0].toInt())
                }
            }

            Hub3ItemCode.HUB3_ITEM_CODE_LED_DUTY.value -> {
                L.d("harry", "[收]HUB3_ITEM_CODE_LED_DUTY:" + receivePayload.payload[0].toUByte())
                hub3Brightness = receivePayload.payload[0]
                (delegate as? CHHub3Delegate)?.onHub3BrightnessReceive(this, receivePayload.payload[0].toInt())
            }

            else -> {
                L.d(
                    "hcia", msg = "!![hub3][pub][${receivePayload.cmdItCode}]"
                )
            }
        }
    }

    override fun getIrLearnedData(result: CHResult<ByteArray>) {
        val topic = "hub3/${this.deviceId.toString().uppercase()}/ir/learned/data"
        L.d("getIrLearnedData", "topic: $topic")
        CHIotManager.subscribeTopic(topic) { it ->
            it.onSuccess {
                L.d("getIrLearnedData", "收到 " + it.data.size + " 个字节的红外数据")
                CHIotManager.unsubscribeTopic(topic) // 收到数据后， 取消订阅
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(it.data)))
            }
        }
    }

}
