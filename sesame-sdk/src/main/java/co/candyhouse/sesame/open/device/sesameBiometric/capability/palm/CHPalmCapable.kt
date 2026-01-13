package co.candyhouse.sesame.open.device.sesameBiometric.capability.palm

import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHDataSynchronizeCapable
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.server.dto.CHPalmNameRequest

interface CHPalmCapable {
    fun palmModeSet(mode: Byte, result: CHResult<CHEmpty>)
    fun palmModeGet(result: CHResult<Byte>)
    fun palmListGet(result: CHResult<CHEmpty>)
    fun palmDelete(palmID: String, deviceId: String, result: CHResult<CHEmpty>)
    fun getPalmDataSyncCapable(): CHDataSynchronizeCapable

    fun registerEventDelegate(device: CHSesameBiometricBase, delegate: CHPalmDelegate)
    fun unregisterEventDelegate(device: CHSesameBiometricBase, delegate: CHPalmDelegate)
}