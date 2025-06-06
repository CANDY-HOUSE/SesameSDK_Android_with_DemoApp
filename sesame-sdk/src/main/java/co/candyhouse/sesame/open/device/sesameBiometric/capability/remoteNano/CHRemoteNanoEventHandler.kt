package co.candyhouse.sesame.open.device.sesameBiometric.capability.remoteNano

import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.os3.CHRemoteNanoTriggerSettings
import co.candyhouse.sesame.ble.os3.biometric.touchPro.CHSesameTouchPro
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHEventHandler

class CHRemoteNanoEventHandler(private val delegate: CHRemoteNanoDelegate?) : CHEventHandler {
    override fun handleEvent(device: CHSesameConnector, payload: SSM3PublishPayload): Boolean {
        if (delegate == null) return false

        if (payload.cmdItCode == SesameItemCode.REMOTE_NANO_ITEM_CODE_PUB_TRIGGER_DELAYTIME.value) {
            val triggerDelaySetting = CHRemoteNanoTriggerSettings.fromData(payload.payload)
            if (device is CHSesameTouchPro) {
                device.triggerDelaySetting = triggerDelaySetting
                triggerDelaySetting?.let { delegate.onTriggerDelaySecondReceived(device, it) }
            }
            return true
        } else {
            return false
        }
    }
}