package co.candyhouse.sesame.open.devices.sesameBiometric.capability.face

import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.sesameBiometric.parseData.CHSesameTouchFace

interface CHFaceDelegate {
    fun onFaceModeChanged(device: CHDevices, mode: Byte) {}
    fun onFaceReceiveStart(device: CHDevices) {}
    fun onFaceReceive(device: CHDevices, face: CHSesameTouchFace) {}
    fun onFaceChanged(device: CHDevices, face: CHSesameTouchFace) {}
    fun onFaceReceiveEnd(device: CHDevices) {}
    fun onFaceDeleted(device: CHDevices, faceID: Byte, isSuccess: Boolean) {}
}