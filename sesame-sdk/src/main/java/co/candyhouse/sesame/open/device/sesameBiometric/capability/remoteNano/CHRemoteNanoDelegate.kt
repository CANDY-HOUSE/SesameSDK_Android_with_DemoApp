package co.candyhouse.sesame.open.device.sesameBiometric.capability.remoteNano

import co.candyhouse.sesame.ble.os3.CHRemoteNanoTriggerSettings
import co.candyhouse.sesame.open.device.CHSesameConnector

interface CHRemoteNanoDelegate {
    fun onTriggerDelaySecondReceived(
        device: CHSesameConnector,
        setting: CHRemoteNanoTriggerSettings
    )
}