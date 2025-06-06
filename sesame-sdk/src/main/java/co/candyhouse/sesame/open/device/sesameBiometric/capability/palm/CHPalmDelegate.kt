package co.candyhouse.sesame.open.device.sesameBiometric.capability.palm

import co.candyhouse.sesame.ble.os3.CHSesameTouchFace
import co.candyhouse.sesame.open.device.CHSesameConnector

interface CHPalmDelegate {
    fun onPalmModeChanged(device: CHSesameConnector, mode: Byte) {}
    fun onPalmReceive(device: CHSesameConnector, tochface: CHSesameTouchFace) {}
    fun onPalmChanged(device: CHSesameConnector, tochface: CHSesameTouchFace) {}
    fun onPalmReceiveStart(device: CHSesameConnector) {}
    fun onPalmReceiveEnd(device: CHSesameConnector) {}
    fun onPalmDeleted(device: CHSesameConnector, palmID: Byte, isSuccess: Boolean) {}
}