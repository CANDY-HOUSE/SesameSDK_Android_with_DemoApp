package co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale

import co.candyhouse.sesame.ble.SSM3ResponsePayload
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.open.CHResult

interface CHCapabilitySupport {
    fun sendCommand(payload: SesameOS3Payload, callback: (SSM3ResponsePayload) -> Unit)
    fun <T> isBleAvailable(result: CHResult<T>): Boolean
}