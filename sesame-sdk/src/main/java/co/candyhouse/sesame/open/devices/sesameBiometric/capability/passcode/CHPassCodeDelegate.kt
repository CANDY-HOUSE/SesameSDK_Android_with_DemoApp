package co.candyhouse.sesame.open.devices.sesameBiometric.capability.passcode

import co.candyhouse.sesame.open.devices.base.CHDevices

interface CHPassCodeDelegate {
    fun onKeyBoardReceive(device: CHDevices, ID: String, hexName: String, type: Byte) {}
    fun onKeyBoardChanged(device: CHDevices, ID: String, hexName: String, type: Byte) {}
    fun onKeyBoardReceiveEnd(device: CHDevices) {}
    fun onKeyBoardReceiveStart(device: CHDevices) {}
    fun onKeyBoardModeChange(device: CHDevices, mode: Byte) {}
    fun onKeyBoardDelete(device: CHDevices, ID: String) {}
}