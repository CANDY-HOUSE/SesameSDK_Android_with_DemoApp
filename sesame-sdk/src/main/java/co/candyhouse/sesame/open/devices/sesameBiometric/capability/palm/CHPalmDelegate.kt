package co.candyhouse.sesame.open.devices.sesameBiometric.capability.palm

import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.sesameBiometric.parseData.CHSesameTouchFace

interface CHPalmDelegate {
    fun onPalmModeChanged(device: CHDevices, mode: Byte) {}
    fun onPalmReceive(device: CHDevices, tochface: CHSesameTouchFace) {}
    fun onPalmChanged(device: CHDevices, tochface: CHSesameTouchFace) {}
    fun onPalmReceiveStart(device: CHDevices) {}
    fun onPalmReceiveEnd(device: CHDevices) {}
    fun onPalmDeleted(device: CHDevices, palmID: Byte, isSuccess: Boolean) {}
}