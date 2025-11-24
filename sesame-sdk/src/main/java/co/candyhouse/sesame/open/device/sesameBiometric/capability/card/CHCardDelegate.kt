package co.candyhouse.sesame.open.device.sesameBiometric.capability.card

import co.candyhouse.sesame.open.device.CHSesameConnector

interface CHCardDelegate {
    fun onCardReceive(device: CHSesameConnector, cardID: String, hexName: String, type: Byte) {}
    fun onCardChanged(device: CHSesameConnector, cardID: String, hexName: String, type: Byte) {}
    fun onCardReceiveEnd(device: CHSesameConnector) {}
    fun onCardReceiveStart(device: CHSesameConnector) {}
    fun onCardModeChanged(device: CHSesameConnector, mode: Byte) {}
    fun onCardDelete(device: CHSesameConnector, cardID: String) {}
}