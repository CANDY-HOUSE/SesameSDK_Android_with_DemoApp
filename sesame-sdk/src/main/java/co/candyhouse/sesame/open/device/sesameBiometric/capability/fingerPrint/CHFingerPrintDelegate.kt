package co.candyhouse.sesame.open.device.sesameBiometric.capability.fingerPrint

import co.candyhouse.sesame.open.device.CHSesameConnector

interface CHFingerPrintDelegate {
    fun onFingerPrintReceive(device: CHSesameConnector, ID: String, hexName: String, type: Byte) {}
    fun onFingerPrintChanged(device: CHSesameConnector, ID: String, hexName: String, type: Byte) {}
    fun onFingerPrintReceiveEnd(device: CHSesameConnector) {}
    fun onFingerPrintReceiveStart(device: CHSesameConnector) {}
    fun onFingerModeChange(device: CHSesameConnector, mode: Byte) {}
    fun onFingerDelete(device: CHSesameConnector, ID: String) {}
}