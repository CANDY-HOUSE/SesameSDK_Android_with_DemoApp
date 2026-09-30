package co.candyhouse.sesame.ble.os3.sesame5

import android.annotation.SuppressLint
import co.candyhouse.sesame.ble.CHDeviceStatus
import co.candyhouse.sesame.ble.DeviceSegmentType
import co.candyhouse.sesame.ble.NSError
import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.SSM3ResponsePayload
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.SesameResultCode
import co.candyhouse.sesame.ble.historyTagBLE
import co.candyhouse.sesame.ble.isBleAvailable
import co.candyhouse.sesame.ble.os3.base.CHSesameOS3LockBase
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.utils.EccKey
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.toHexString
import co.candyhouse.sesame.utils.toReverseBytes

@SuppressLint("MissingPermission")
internal class CHSesame5Device : CHSesameOS3LockBase(), CHSesame5 {

    override var mechSetting: CHSesame5MechSettings? = null
    override var opsSetting: CHSesame5OpsSettings? = null

    override fun configureLockPosition(lockTarget: Short, unlockTarget: Short, result: CHResult<CHEmpty>) {
        val cmd = SesameOS3Payload(
            SesameItemCode.mechSetting.value,
            lockTarget.toReverseBytes() + unlockTarget.toReverseBytes()
        )
        sendCommand(cmd, DeviceSegmentType.cipher) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                mechSetting?.lockPosition = lockTarget
                mechSetting?.unlockPosition = unlockTarget
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }

    override fun sendAdvProductTypeCommand(data: ByteArray, result: CHResult<CHEmpty>) {
        val cmd = SesameOS3Payload(SesameItemCode.SS3_ITEM_CODE_SET_ADV_PRODUCT_TYPE.value, data)
        sendCommand(cmd, DeviceSegmentType.cipher) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }

    override fun autolock(delay: Int, result: CHResult<Int>) {
        if (!isBleAvailable(result)) return
        sendCommand(
            SesameOS3Payload(SesameItemCode.autolock.value, delay.toShort().toReverseBytes()),
            DeviceSegmentType.cipher
        ) { response ->
            if (response.cmdResultCode == SesameResultCode.success.value) {
                mechSetting?.autoLockSecond = delay.toShort()
                result.invoke(Result.success(CHResultState.CHResultStateBLE(delay)))
            } else result.invoke(Result.failure(NSError(response.cmdResultCode.toString(), "CBCentralManager", response.cmdResultCode.toInt())))
        }
    }

    override fun opSensorControl(isEnable: Int, result: CHResult<Int>) {
        if (!isBleAvailable(result)) return
        sendCommand(
            SesameOS3Payload(SesameItemCode.OPS_CONTROL.value, isEnable.toShort().toReverseBytes()),
            DeviceSegmentType.cipher
        ) { response ->
            if (response.cmdResultCode == SesameResultCode.success.value) {
                opsSetting?.opsLockSecond = isEnable.toUShort()
                result.invoke(Result.success(CHResultState.CHResultStateBLE(isEnable)))
            } else result.invoke(Result.failure(NSError(response.cmdResultCode.toString(), "CBCentralManager", response.cmdResultCode.toInt())))
        }
    }

    override fun magnet(result: CHResult<CHEmpty>) {
        if (!isBleAvailable(result)) return
        sendCommand(
            SesameOS3Payload(SesameItemCode.magnet.value, byteArrayOf()),
            DeviceSegmentType.cipher
        ) { response ->
            if (response.cmdResultCode == SesameResultCode.success.value) result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            else result.invoke(Result.failure(NSError(response.cmdResultCode.toString(), "CBCentralManager", response.cmdResultCode.toInt())))
        }
    }

    override fun toggle(historytag: ByteArray?, result: CHResult<CHEmpty>) {
        if (!isBleAvailable(result)) return
        if (deviceStatus == CHDeviceStatus.Locked) unlock(historytag, result) else lock(historytag, result)
    }

    override fun unlock(historytag: ByteArray?, result: CHResult<CHEmpty>) {

        if (!isBleAvailable(result)) return
        sendCommand(
            SesameOS3Payload(SesameItemCode.unlock.value, historyTagBLE(historytag)),
            DeviceSegmentType.cipher
        ) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }

    override fun lock(historytag: ByteArray?, result: CHResult<CHEmpty>) {

        if (!isBleAvailable(result)) return
        sendCommand(
            SesameOS3Payload(SesameItemCode.lock.value, historyTagBLE(historytag)),
            DeviceSegmentType.cipher
        ) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
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
        mechStatus = CHSesame5MechStatus(registerRes.payload.sliceArray(0..6))
        mechSetting = CHSesame5MechSettings(registerRes.payload.sliceArray(7..12))
        val eccPublicKeyFromSS5 = registerRes.payload.sliceArray(13..76)
        val ecdhSecret = EccKey.ecdh(eccPublicKeyFromSS5)
        val deviceSecret = ecdhSecret.sliceArray(0..15).toHexString()

        saveDeviceAndCipher(deviceSecret, serverSecret, result)
        deviceStatus = if (mechStatus?.isInLockRange == true) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
    }

    override fun handleDevicePublish(receivePayload: SSM3PublishPayload) {
        L.d("onGattSesamePublish", "[ss5] ${receivePayload.cmdItCode}, data: 0x${receivePayload.payload.toHexString()}")

        when (receivePayload.cmdItCode) {
            SesameItemCode.mechStatus.value -> {
                mechStatus = CHSesame5MechStatus(receivePayload.payload)
                deviceStatus = if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
                reportBatteryData(receivePayload.payload.sliceArray(0..1).toHexString())
            }

            SesameItemCode.mechSetting.value -> {
                mechSetting = CHSesame5MechSettings(receivePayload.payload)
            }

            SesameItemCode.OPS_CONTROL.value -> {
                opsSetting = CHSesame5OpsSettings(receivePayload.payload)
                L.d("switch", "[ss5][opsSecond]:${opsSetting!!.opsLockSecond}")
            }
        }
    }
}
