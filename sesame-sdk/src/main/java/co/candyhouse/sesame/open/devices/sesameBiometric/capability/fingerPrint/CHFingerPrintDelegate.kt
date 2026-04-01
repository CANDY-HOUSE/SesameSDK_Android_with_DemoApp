package co.candyhouse.sesame.open.devices.sesameBiometric.capability.fingerPrint

import co.candyhouse.sesame.open.devices.base.CHDevices

interface CHFingerPrintDelegate {
    fun onFingerPrintReceive(device: CHDevices, ID: String, hexName: String, type: Byte) {}
    fun onFingerPrintChanged(device: CHDevices, ID: String, hexName: String, type: Byte) {}
    fun onFingerPrintReceiveEnd(device: CHDevices) {}
    fun onFingerPrintReceiveStart(device: CHDevices) {}
    fun onFingerModeChange(device: CHDevices, mode: Byte) {}
    fun onFingerDelete(device: CHDevices, ID: String) {}
}