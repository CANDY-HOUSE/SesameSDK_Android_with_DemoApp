package co.candyhouse.sesame.ble.os3

import androidx.lifecycle.LiveData
import co.candyhouse.sesame.ble.DeviceSegmentType
import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.SSM3ResponsePayload
import co.candyhouse.sesame.ble.isBleAvailable
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHEventHandler
import co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint.CHFingerPrintCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint.CHFingerPrintCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.capability.remoteNano.CHRemoteNanoCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.remoteNano.CHRemoteNanoCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.Event

/**
 * Bike3
 *
 * @author frey on 2026/2/2
 */
internal class CHSesameBike3Device :
    CHSesameBike2Device(),
    CHSesameBiometricBase,
    CHRemoteNanoCapable by CHRemoteNanoCapableImpl(),
    CHFingerPrintCapable by CHFingerPrintCapableImpl() {

    override var triggerDelaySetting: CHRemoteNanoTriggerSettings? = null
    override var ssm2KeysMap: MutableMap<String, ByteArray> = mutableMapOf()
    override var radarPayload: ByteArray = byteArrayOf()

    override fun getSSM2KeysLiveData(): LiveData<Map<String, ByteArray>>? = null
    override fun getSSM2SlotFullLiveData(): LiveData<Event<Boolean>>? = null
    override fun getSSM2SupportLiveDataLiveData(): LiveData<Event<Boolean>>? = null

    override fun insertSesame(sesame: CHDevices, result: CHResult<CHEmpty>) {}

    override fun removeSesame(tag: String, result: CHResult<CHEmpty>) {}

    override fun sendCommand(payload: SesameOS3Payload, callback: (SSM3ResponsePayload) -> Unit) {
        super.sendCommand(payload, DeviceSegmentType.cipher, callback)
    }

    override fun <T> isBleAvailable(result: CHResult<T>): Boolean {
        return (this as CHDevices).isBleAvailable(result)
    }

    private val biometricEventHandlers = mutableListOf<CHEventHandler>()

    override fun addEventHandler(handler: CHEventHandler) {
        biometricEventHandlers.add(handler)
    }

    override fun removeEventHandler(handler: CHEventHandler) {
        biometricEventHandlers.remove(handler)
    }

    override fun clearEventHandlers() {
        biometricEventHandlers.clear()
    }

    override fun onGattSesamePublish(receivePayload: SSM3PublishPayload) {
        super.onGattSesamePublish(receivePayload)

        for (h in biometricEventHandlers.toList()) {
            if (h.handleEvent(this, receivePayload)) break
        }
    }
}