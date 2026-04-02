package co.candyhouse.sesame.open.devices.sesameBiometric.capability.connect

import co.candyhouse.sesame.open.devices.base.CHSesameConnector

interface CHDeviceConnectDelegate {
    fun onSSM2KeysChanged(device: CHSesameConnector, ssm2keys: Map<String, ByteArray>)
    fun onRadarReceive(device: CHSesameConnector, payload: ByteArray) {}
}