package co.candyhouse.sesame.ble.os3.biometric.capability.palm

import co.candyhouse.sesame.ble.CHDevices
import co.candyhouse.sesame.ble.os3.biometric.parseData.CHSesameTouchFace

interface CHPalmDelegate {
    fun onPalmModeChanged(device: CHDevices, mode: Byte) {}
    fun onPalmReceive(device: CHDevices, tochface: CHSesameTouchFace) {}
    fun onPalmChanged(device: CHDevices, tochface: CHSesameTouchFace) {}
    fun onPalmReceiveStart(device: CHDevices) {}
    fun onPalmReceiveEnd(device: CHDevices) {}
    fun onPalmDeleted(device: CHDevices, palmID: Byte, isSuccess: Boolean) {}
}
