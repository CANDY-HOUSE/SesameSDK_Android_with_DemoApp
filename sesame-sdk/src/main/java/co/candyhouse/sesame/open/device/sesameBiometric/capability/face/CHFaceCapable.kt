package co.candyhouse.sesame.open.device.sesameBiometric.capability.face

import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHDataSynchronizeCapable
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.server.dto.CHFaceNameRequest

interface CHFaceCapable {
    fun faceModeSet(mode: Byte, result: CHResult<CHEmpty>)
    fun faceModeGet(result: CHResult<Byte>)
    fun faceListGet(result: CHResult<CHEmpty>)
    fun faceDelete(faceID: String, deviceID: String, result: CHResult<CHEmpty>)
    fun faceNameGet(faceID: String, faceNameUUID: String, subUUID: String, deviceUUID: String, result: CHResult<String>)
    fun faceNameSet(faceNameRequest: CHFaceNameRequest, result: CHResult<String>)
    fun getFaceDataSyncCapable(): CHDataSynchronizeCapable

    fun registerEventDelegate(device: CHSesameBiometricBase, delegate: CHFaceDelegate)
    fun unregisterEventDelegate(device: CHSesameBiometricBase, delegate: CHFaceDelegate)

}