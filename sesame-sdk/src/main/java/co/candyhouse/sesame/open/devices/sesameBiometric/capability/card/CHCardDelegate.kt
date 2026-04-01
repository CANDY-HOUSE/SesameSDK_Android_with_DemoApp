package co.candyhouse.sesame.open.devices.sesameBiometric.capability.card

import co.candyhouse.sesame.open.devices.base.CHDevices

interface CHCardDelegate {
    fun onCardReceive(device: CHDevices, cardID: String, hexName: String, type: Byte) {}
    fun onCardChanged(device: CHDevices, cardID: String, hexName: String, type: Byte) {}
    fun onCardReceiveEnd(device: CHDevices) {}
    fun onCardReceiveStart(device: CHDevices) {}
    fun onCardModeChanged(device: CHDevices, mode: Byte) {}
    fun onCardDelete(device: CHDevices, cardID: String) {}
}