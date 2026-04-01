package co.candyhouse.sesame.open.devices.sesameBiometric.capability.remoteNano

import co.candyhouse.sesame.open.devices.base.CHSesameConnector
import co.candyhouse.sesame.open.devices.sesameBiometric.parseData.CHRemoteNanoTriggerSettings

interface CHRemoteNanoDelegate {
    fun onTriggerDelaySecondReceived(
        device: CHSesameConnector,
        setting: CHRemoteNanoTriggerSettings
    )
}