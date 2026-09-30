package co.candyhouse.sesame.ble.os3.biometric.capability.card

import co.candyhouse.sesame.ble.CHDevices

interface CHCardDelegate {
    fun onCardReceive(device: CHDevices, cardID: String, hexName: String, type: Byte) {}
    fun onCardChanged(device: CHDevices, cardID: String, hexName: String, type: Byte) {}
    fun onCardReceiveEnd(device: CHDevices) {}
    fun onCardReceiveStart(device: CHDevices) {}
    fun onCardModeChanged(device: CHDevices, mode: Byte) {}
    fun onCardDelete(device: CHDevices, cardID: String) {}
}
