package co.candyhouse.app.ble

import co.candyhouse.sesame.ble.CHDeviceStatus

/** Web bridge vocabulary shared with iOS; SDK states and connection timing stay unchanged. */
internal fun CHDeviceStatus.webBleState(): String = when (this) {
    CHDeviceStatus.NoBleSignal -> "noBleSignal"
    CHDeviceStatus.ReceivedAdV -> "receivedBle"
    CHDeviceStatus.BleConnecting -> "bleConnecting"
    CHDeviceStatus.Reset -> "reset"
    CHDeviceStatus.WaitingGatt, CHDeviceStatus.DiscoverServices -> "waitingGatt"
    CHDeviceStatus.BleLogining -> "bleLogining"
    CHDeviceStatus.ReadyToRegister -> "readyToRegister"
    CHDeviceStatus.WaitingForAuth -> "waitingForAuth"
    CHDeviceStatus.Registering -> "registering"
    CHDeviceStatus.DfuMode -> "dfumode"
    CHDeviceStatus.Locked -> "locked"
    CHDeviceStatus.Unlocked -> "unlocked"
    CHDeviceStatus.Moved -> "moved"
    CHDeviceStatus.NoSettings -> "noSettings"
    CHDeviceStatus.WaitApConnect -> "waitApConnect"
    CHDeviceStatus.Busy -> "busy"
    // Firmware network states are not BLE hints; Biz uses WebSocket for cloud status.
    CHDeviceStatus.IotConnected, CHDeviceStatus.IotDisconnected -> ""
}
