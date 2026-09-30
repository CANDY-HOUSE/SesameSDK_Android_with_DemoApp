package co.candyhouse.sesame.ble.os3.biometric.capability.passcode

import co.candyhouse.sesame.ble.CHDevices

interface CHPassCodeDelegate {
    fun onKeyBoardReceive(device: CHDevices, ID: String, hexName: String, type: Byte) {}
    fun onKeyBoardChanged(device: CHDevices, ID: String, hexName: String, type: Byte) {}
    fun onKeyBoardReceiveEnd(device: CHDevices) {}
    fun onKeyBoardReceiveStart(device: CHDevices) {}
    fun onKeyBoardModeChange(device: CHDevices, mode: Byte) {}
    fun onKeyBoardDelete(device: CHDevices, ID: String) {}
}
