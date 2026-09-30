package co.candyhouse.sesame.ble.os3.biometric.capability.remoteNano

import co.candyhouse.sesame.ble.os3.biometric.capability.baseCapbale.CHCapabilityHost
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult

interface CHRemoteNanoCapable {
    fun setTriggerDelayTime(time: UByte, result: CHResult<CHEmpty>)

    fun registerEventDelegate(device: CHCapabilityHost, delegate: CHRemoteNanoDelegate)
    fun unregisterEventDelegate(delegate: CHRemoteNanoDelegate)
}
