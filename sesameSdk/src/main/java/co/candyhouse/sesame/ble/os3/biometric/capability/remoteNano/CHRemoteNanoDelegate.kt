package co.candyhouse.sesame.ble.os3.biometric.capability.remoteNano

import co.candyhouse.sesame.ble.CHSesameConnector
import co.candyhouse.sesame.ble.os3.biometric.parseData.CHRemoteNanoTriggerSettings

interface CHRemoteNanoDelegate {
    fun onTriggerDelaySecondReceived(
        device: CHSesameConnector,
        setting: CHRemoteNanoTriggerSettings
    )
}
