package co.candyhouse.sesame.open.devices.sesameBiometric.capability.palm

import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHCapabilityHost
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHDataSynchronizeCapable
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult

interface CHPalmCapable {
    fun palmModeSet(mode: Byte, result: CHResult<CHEmpty>)
    fun palmModeGet(result: CHResult<Byte>)
    fun palmListGet(result: CHResult<CHEmpty>)
    fun palmDelete(palmID: String, deviceId: String, result: CHResult<CHEmpty>)
    fun getPalmDataSyncCapable(): CHDataSynchronizeCapable

    fun registerEventDelegate(device: CHCapabilityHost, delegate: CHPalmDelegate)
    fun unregisterEventDelegate(device: CHCapabilityHost, delegate: CHPalmDelegate)
}