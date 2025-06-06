package co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale

import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.open.device.CHSesameConnector

interface CHEventHandler {
    fun handleEvent(device: CHSesameConnector, payload: SSM3PublishPayload): Boolean
}