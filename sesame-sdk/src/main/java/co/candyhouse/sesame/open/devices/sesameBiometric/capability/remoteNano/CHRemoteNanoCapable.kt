package co.candyhouse.sesame.open.devices.sesameBiometric.capability.remoteNano

import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHCapabilityHost
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult

interface CHRemoteNanoCapable {
    fun setTriggerDelayTime(time: UByte, result: CHResult<CHEmpty>)

    fun registerEventDelegate(device: CHCapabilityHost, delegate: CHRemoteNanoDelegate)
    fun unregisterEventDelegate(delegate: CHRemoteNanoDelegate)
}