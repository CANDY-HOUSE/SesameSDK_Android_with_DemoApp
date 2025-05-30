package co.candyhouse.sesame.open.device.sesameBiometric.capability.palm

import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.server.dto.CHPalmNameRequest

interface CHPalmCapable {
    fun palmModeSet(mode: Byte, result: CHResult<CHEmpty>)
    fun palmModeGet(result: CHResult<Byte>)
    fun palmListGet(result: CHResult<CHEmpty>)
    fun palmChange(ID: String, name: String, result: CHResult<CHEmpty>)
    fun palmDelete(ID: Int, result: CHResult<CHEmpty>)
    fun palmNameGet(palmID: String, palmNameUUID: String, subUUID: String, deviceUUID: String, result: CHResult<String>)
    fun palmNameSet(palmNameRequest: CHPalmNameRequest, result: CHResult<String>)

    fun registerEventDelegate(device: CHSesameBiometricBase, delegate: CHPalmDelegate)
    fun unregisterEventDelegate(device: CHSesameBiometricBase, delegate: CHPalmDelegate)
}