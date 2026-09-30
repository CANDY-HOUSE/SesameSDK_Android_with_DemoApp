package co.candyhouse.sesame.ble.os3.biometric.capability.baseCapbale

import co.candyhouse.sesame.ble.SSM3ResponsePayload
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.utils.CHResult

interface CHCapabilitySupport {
    fun sendCommand(payload: SesameOS3Payload, callback: (SSM3ResponsePayload) -> Unit)
    fun <T> isBleAvailable(result: CHResult<T>): Boolean
}
