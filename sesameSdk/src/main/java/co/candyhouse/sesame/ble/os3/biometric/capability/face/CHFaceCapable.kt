package co.candyhouse.sesame.ble.os3.biometric.capability.face

import co.candyhouse.sesame.ble.os3.biometric.capability.baseCapbale.CHCapabilityHost
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult

interface CHFaceCapable {
    fun faceModeSet(mode: Byte, result: CHResult<CHEmpty>)
    fun faceModeGet(result: CHResult<Byte>)
    fun faceListGet(result: CHResult<CHEmpty>)
    fun faceDelete(faceID: String, deviceID: String, result: CHResult<CHEmpty>)

    fun registerEventDelegate(device: CHCapabilityHost, delegate: CHFaceDelegate)
    fun unregisterEventDelegate(device: CHCapabilityHost, delegate: CHFaceDelegate)

}
