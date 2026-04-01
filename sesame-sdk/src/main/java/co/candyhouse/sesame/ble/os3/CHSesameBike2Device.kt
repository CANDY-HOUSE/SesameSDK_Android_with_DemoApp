package co.candyhouse.sesame.ble.os3

import android.annotation.SuppressLint
import co.candyhouse.sesame.ble.DeviceSegmentType
import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.SSM3ResponsePayload
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.isBleAvailable
import co.candyhouse.sesame.ble.os3.base.SesameOS3BleCipher
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.db.model.historyTagBLE
import co.candyhouse.sesame.db.model.historyTagIOT
import co.candyhouse.sesame.open.devices.CHSesame2MechStatus
import co.candyhouse.sesame.open.devices.CHSesame5MechSettings
import co.candyhouse.sesame.open.devices.CHSesame5MechStatus
import co.candyhouse.sesame.open.devices.CHSesameBike2
import co.candyhouse.sesame.open.devices.CHSesameBike2MechStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceLoginStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceStatus
import co.candyhouse.sesame.open.devices.base.CHSesameOS3LockBase
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.CHIotManager
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.utils.EccKey
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.toHexString

@SuppressLint("MissingPermission")
internal open class CHSesameBike2Device : CHSesameOS3LockBase(), CHSesameBike2 {

    override var mechSetting: CHSesame5MechSettings? = null

    override fun goIOT() {
        CHIotManager.subscribeSesame2Shadow(this) { result ->
            result.onSuccess { resource ->
                L.d("hcia", "[bike2][iot]")
                L.d("hcia", "🥝 [bike2]ss5_shadow裡存的hub3列表:${resource.data.state.reported.wm2s}")

                resource.data.state.reported.wm2s?.let { wm2s ->
                    L.d("hcia", "[bike2]wm2s:$wm2s")
                    isConnectedByWM2 = wm2s.map { it.value.hexStringToByteArray().first().toInt() }.contains(1)
                }

                if (isConnectedByWM2) {
                    resource.data.state.reported.mechst?.let { mechShadow ->
                        L.d("harry", "[bike2][iot] mechShadow【${mechShadow.hexStringToByteArray().size}】: $mechShadow")
                        val bytes = mechShadow.hexStringToByteArray()
                        mechStatus = if (bytes.size >= 7) {
                            CHSesame5MechStatus(CHSesame2MechStatus(bytes).ss5Adapter())
                        } else {
                            CHSesameBike2MechStatus(bytes.sliceArray(0..2))
                        }
                        L.d("harry", "[bike2]mechStatus isInLockRange: ${mechStatus!!.isInLockRange}")
                    }
                    deviceShadowStatus =
                        if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
                } else {
                    deviceShadowStatus = null
                }
            }
        }
    }

    override fun unlock(historytag: ByteArray?, result: CHResult<CHEmpty>) {
        if (deviceStatus.value == CHDeviceLoginStatus.logined && isBleAvailable(result)) {
            sendCommand(
                SesameOS3Payload(SesameItemCode.unlock.value, sesame2KeyData!!.historyTagBLE(historytag)),
                DeviceSegmentType.cipher
            ) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
        } else {
            CHAPIClientBiz.cmdSesame(
                SesameItemCode.unlock,
                this,
                sesame2KeyData!!.historyTagIOT(historytag),
                result
            )
        }
    }

    override fun onHistoryReceived(historyData: ByteArray) {}

    override fun onHistoryReceivedInternal(historyData: ByteArray) {
        onHistoryReceived(historyData)
    }

    override fun handleRegisterResponse(
        registerRes: SSM3ResponsePayload,
        serverSecret: String,
        result: CHResult<CHEmpty>
    ) {
        try {
            val eccPublicKeyFromSS5 = registerRes.payload.sliceArray(13..76)
            val ecdhSecret = EccKey.ecdh(eccPublicKeyFromSS5)

            mechStatus = CHSesame5MechStatus(registerRes.payload.sliceArray(0..6))
            mechSetting = CHSesame5MechSettings(registerRes.payload.sliceArray(7..12))

            val deviceSecret = ecdhSecret.sliceArray(0..15).toHexString()
            saveDeviceAndCipher(deviceSecret, serverSecret, result)
            deviceStatus = if (mechStatus?.isInLockRange == true) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
        } catch (e: Exception) {
            mechStatus = CHSesameBike2MechStatus(registerRes.payload.sliceArray(0..2))
            deviceStatus = if (mechStatus?.isInLockRange == true) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked

            val ecdhSecretPre16 = EccKey.ecdh(registerRes.payload.sliceArray(3..66)).sliceArray(0..15)
            sesame2KeyData = CHDevice(
                deviceId.toString(),
                productModel.deviceModel(),
                null,
                "0000",
                ecdhSecretPre16.toHexString(),
                serverSecret
            )
            cipher = SesameOS3BleCipher(
                "customDeviceName",
                AesCmac(ecdhSecretPre16, 16).computeMac(mSesameToken)!!,
                ("00" + mSesameToken.toHexString()).hexStringToByteArray()
            )
            CHDB.CHSS2Model.insert(sesame2KeyData!!) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
        }
    }

    override fun handleDevicePublish(receivePayload: SSM3PublishPayload) {
        L.d("harry", "CHSesameBike2Device onGattSesamePublish: ${receivePayload.cmdItCode} payload: ${receivePayload.payload.toHexString()}")

        when (receivePayload.cmdItCode) {
            SesameItemCode.mechStatus.value -> {
                L.d("harry", "[bike2]mechStatus【size: ${receivePayload.payload.size}】: ${receivePayload.payload.toHexString()}")
                mechStatus = when (receivePayload.payload.size) {
                    7 -> CHSesame5MechStatus(receivePayload.payload)
                    3 -> CHSesameBike2MechStatus(receivePayload.payload)
                    else -> mechStatus
                }
                if (mechStatus != null) {
                    L.d("harry", "[bike2]mechStatus isInLockRange: ${mechStatus!!.isInLockRange}")
                    deviceStatus = if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
                    reportBatteryData(receivePayload.payload.sliceArray(0..1).toHexString())
                }
            }
        }
    }
}