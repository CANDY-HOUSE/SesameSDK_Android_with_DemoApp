package co.candyhouse.sesame.ble.os3

import android.annotation.SuppressLint
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.CHadv
import co.candyhouse.sesame.ble.DeviceSegmentType
import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.SesameItemCode
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
import co.candyhouse.sesame.open.device.CHSesame5MechStatus
import co.candyhouse.sesame.open.device.CHSesameBike2
import co.candyhouse.sesame.open.device.CHSesameBike2MechStatus
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
internal class CHSesameBike2Device : CHSesameOS3(), CHSesameBike2, CHDeviceUtil {
    /** 設備廣播 */
    override var advertisement: CHadv? = null
        set(value) {
            field = value
            parceADV(value)
        }


    /** 聯網處理 */
    var isConnectedByWM2: Boolean = false
    override fun goIOT() {
        CHIotManager.subscribeSesame2Shadow(this) { result ->
            result.onSuccess { resource ->
                L.d("hcia", "[bike2][iot]")
                L.d("hcia", "\uD83E\uDD5D [bike2]ss5_shadow裡存的hub3列表:" + resource.data.state.reported.wm2s)
                resource.data.state.reported.wm2s?.let { wm2s ->
                    L.d("hcia", "[bike2]wm2s:$wm2s")
                    isConnectedByWM2 = wm2s.map { it.value.hexStringToByteArray().first().toInt() }.contains(1)
                }

                if (isConnectedByWM2) {
                    resource.data.state.reported.mechst?.let { mechShadow ->
                        mechStatus = CHSesameBike2MechStatus(mechShadow.hexStringToByteArray().sliceArray(0..2))
                        L.d("hcia", "[bike2]mechStatus isInUnlockRange: " + mechStatus!!.isInUnlockRange.toString())
                    }

                    deviceShadowStatus = if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
                } else {
                    deviceShadowStatus = null
                }

            }
        }

    }

    /** 指令發送  */
    override fun unlock(tag: ByteArray?, result: CHResult<CHEmpty>) {
        if (deviceStatus.value == CHDeviceLoginStatus.Login && isBleAvailable(result)) {
            sendCommand(SesameOS3Payload(SesameItemCode.unlock.value, byteArrayOf()), DeviceSegmentType.cipher) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
        } else {
            CHAccountManager.cmdSesame(SesameItemCode.unlock, this, byteArrayOf(), result)

        }
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
                L.d("harry", "[bike2]register result【size: ${IRRes.payload.size}】:  ${IRRes.payload.toHexString()}")

                if (IRRes.payload.size == 71) {
                    // 新固件， mechStatus 统一成 SS5 格式 的 7个字节
                    mechStatus = CHSesame5MechStatus(IRRes.payload.toHexString().hexStringToByteArray().sliceArray(0..6))
                    val eccPublicKeyFromSS5 = IRRes.payload.toHexString().hexStringToByteArray().sliceArray(7..70)
                    val ecdhSecret = EccKey.ecdh(eccPublicKeyFromSS5)
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
                } else {
                    // 旧固件， mechStatus 只有 3 个字节
                    /** 根據設備狀態特殊處理 */
                    mechStatus = CHSesameBike2MechStatus(IRRes.payload.toHexString().hexStringToByteArray().sliceArray(0..2))
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

    // 如果这台设备当前没有被 Hub3 连上云端， 则通过 APP 报告给云端。
    private fun reportBatteryData(payloadString: String) {
        L.d("harry", "[ss5][reportBatteryData]:" + isInternetAvailable() + ", " + !isConnectedByWM2 + ", payload: " + payloadString)
        if (isInternetAvailable() && !isConnectedByWM2) {
            CHAccountManager.postBatteryData(deviceId.toString().uppercase(), payloadString) {}
        }
    }

    /** 指令接收 */
    override fun onGattSesamePublish(receivePayload: SSM3PublishPayload) {
        L.d("harry", "CHSesameBike2Device onGattSesamePublish: ${receivePayload.cmdItCode} payload: ${receivePayload.payload.toHexString()}")
        super.onGattSesamePublish(receivePayload)
        if (receivePayload.cmdItCode == SesameItemCode.SSM3_ITEM_CODE_BATTERY_VOLTAGE.value) {
            reportBatteryData(receivePayload.payload.toHexString())
        }
        if (receivePayload.cmdItCode == SesameItemCode.mechStatus.value) {
            L.d("harry", "[bike2]mechStatus【size: ${receivePayload.payload.size}】: ${receivePayload.payload.toHexString()}")
            if (receivePayload.payload.size == 7) {
                // 新固件， mechStatus 统一成 SS5 格式 的 7个字节
                mechStatus = CHSesame5MechStatus(receivePayload.payload)
            } else if (receivePayload.payload.size == 3) {
                // 旧固件， mechStatus 只有 3 个字节
                mechStatus = CHSesameBike2MechStatus(receivePayload.payload)
            }
            deviceStatus = if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
        }
    }
}