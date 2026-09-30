package co.candyhouse.sesame.ble.os3.biometric.capability.baseCapbale

import co.candyhouse.sesame.ble.CHDevices
import co.candyhouse.sesame.ble.SSM3PublishPayload

interface CHEventHandler {
    fun handleEvent(device: CHDevices, payload: SSM3PublishPayload): Boolean
}
