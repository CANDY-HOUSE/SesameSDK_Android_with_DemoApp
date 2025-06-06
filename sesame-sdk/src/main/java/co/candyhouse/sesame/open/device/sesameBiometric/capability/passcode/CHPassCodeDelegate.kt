package co.candyhouse.sesame.open.device.sesameBiometric.capability.passcode

import co.candyhouse.sesame.open.device.CHSesameConnector

interface CHPassCodeDelegate {
    fun onKeyBoardReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {}
    fun onKeyBoardChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {}
    fun onKeyBoardReceiveEnd(device: CHSesameConnector) {}
    fun onKeyBoardReceiveStart(device: CHSesameConnector) {}
    fun onKeyBoardModeChange(device: CHSesameConnector, mode: Byte) {}
    fun onKeyBoardDelete(device: CHSesameConnector, ID: String) {}
}