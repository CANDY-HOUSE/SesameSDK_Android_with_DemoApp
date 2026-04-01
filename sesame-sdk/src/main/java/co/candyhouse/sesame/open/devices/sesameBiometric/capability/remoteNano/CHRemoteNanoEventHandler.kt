package co.candyhouse.sesame.open.devices.sesameBiometric.capability.remoteNano

import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.open.devices.CHSesameBiometricDevice
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.isRemote
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHEventHandler
import co.candyhouse.sesame.open.devices.sesameBiometric.parseData.CHRemoteNanoTriggerSettings

class CHRemoteNanoEventHandler(private val delegate: CHRemoteNanoDelegate?) : CHEventHandler {
    override fun handleEvent(device: CHDevices, payload: SSM3PublishPayload): Boolean {
        if (delegate == null) return false

        if (payload.cmdItCode == SesameItemCode.REMOTE_NANO_ITEM_CODE_PUB_TRIGGER_DELAYTIME.value) {
            val triggerDelaySetting = CHRemoteNanoTriggerSettings.fromData(payload.payload)
            if (device.isRemote()) {
                (device as CHSesameBiometricDevice).triggerDelaySetting = triggerDelaySetting
                triggerDelaySetting?.let { delegate.onTriggerDelaySecondReceived(device, it) }
            }
            return true
        } else {
            return false
        }
    }
}