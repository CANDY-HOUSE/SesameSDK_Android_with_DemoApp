package co.candyhouse.sesame.ble.os3

import co.candyhouse.sesame.ble.DeviceSegmentType
import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.SSM3ResponsePayload
import co.candyhouse.sesame.ble.isBleAvailable
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHCapabilityHost
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHEventHandler
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.fingerPrint.CHFingerPrintCapable
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.fingerPrint.CHFingerPrintCapableImpl
import co.candyhouse.sesame.utils.CHResult

/**
 * Bike3
 *
 * @author frey on 2026/2/2
 */
internal class CHSesameBike3Device(
    private val fingerPrintCapability: CHFingerPrintCapableImpl = CHFingerPrintCapableImpl()
) : CHSesameBike2Device(),
    CHCapabilityHost,
    CHFingerPrintCapable by fingerPrintCapability {

    private val fingerPrintEventHandlers = mutableListOf<CHEventHandler>()

    init {
        fingerPrintCapability.setupSupport(this)
    }

    override fun addEventHandler(handler: CHEventHandler) {
        fingerPrintEventHandlers.add(handler)
    }

    override fun removeEventHandler(handler: CHEventHandler) {
        fingerPrintEventHandlers.remove(handler)
    }

    override fun clearEventHandlers() {
        fingerPrintEventHandlers.clear()
    }

    override fun sendCommand(payload: SesameOS3Payload, callback: (SSM3ResponsePayload) -> Unit) {
        super.sendCommand(payload, DeviceSegmentType.cipher, callback)
    }

    override fun <T> isBleAvailable(result: CHResult<T>): Boolean {
        return (this as CHDevices).isBleAvailable(result)
    }

    override fun handleDevicePublish(receivePayload: SSM3PublishPayload) {
        super.handleDevicePublish(receivePayload)
        fingerPrintEventHandlers.toList().forEach { handler ->
            handler.handleEvent(this, receivePayload)
        }
    }
}