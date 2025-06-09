package co.candyhouse.sesame.open.device.sesameBiometric.capability.remoteNano

import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.server.dto.CHEmpty

interface CHRemoteNanoCapable {
    fun setTriggerDelayTime(time: UByte, result: CHResult<CHEmpty>)

    fun registerEventDelegate(device: CHSesameBiometricBase, delegate: CHRemoteNanoDelegate)
    fun unregisterEventDelegate(delegate: CHRemoteNanoDelegate)
}