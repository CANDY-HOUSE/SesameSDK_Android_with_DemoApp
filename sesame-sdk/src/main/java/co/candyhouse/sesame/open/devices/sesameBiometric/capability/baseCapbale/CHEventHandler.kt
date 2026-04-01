package co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale

import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.open.devices.base.CHDevices

interface CHEventHandler {
    fun handleEvent(device: CHDevices, payload: SSM3PublishPayload): Boolean
}