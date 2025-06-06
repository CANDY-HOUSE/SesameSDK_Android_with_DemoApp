package co.candyhouse.sesame.open.device.sesameBiometric.capability.face

import co.candyhouse.sesame.ble.os3.CHSesameTouchFace
import co.candyhouse.sesame.open.device.CHSesameConnector

interface CHFaceDelegate {
    fun onFaceModeChanged(device: CHSesameConnector, mode: Byte) {}
    fun onFaceReceiveStart(device: CHSesameConnector) {}
    fun onFaceReceive(device: CHSesameConnector, face: CHSesameTouchFace) {}
    fun onFaceChanged(device: CHSesameConnector, face: CHSesameTouchFace) {}
    fun onFaceReceiveEnd(device: CHSesameConnector) {}
    fun onFaceDeleted(device: CHSesameConnector, faceID: Byte, isSuccess: Boolean) {}
}