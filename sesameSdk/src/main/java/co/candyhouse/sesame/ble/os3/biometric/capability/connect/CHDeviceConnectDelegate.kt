package co.candyhouse.sesame.ble.os3.biometric.capability.connect

import co.candyhouse.sesame.ble.CHSesameConnector

interface CHDeviceConnectDelegate {
    fun onSSM2KeysChanged(device: CHSesameConnector, ssm2keys: Map<String, ByteArray>)
}
