package co.candyhouse.sesame.open.device.sesameBiometric.capability.connect

import co.candyhouse.sesame.open.device.CHSesameConnector

interface CHDeviceConnectDelegate {
    fun onSSM2KeysChanged(device: CHSesameConnector, ssm2keys: Map<String, ByteArray>)
    fun onRadarReceive(device: CHSesameConnector, payload: ByteArray){}
}