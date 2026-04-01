package co.candyhouse.sesame.open.devices.sesameBiometric.capability.passcode

import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHCapabilityHost
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHDataSynchronizeCapable
import co.candyhouse.sesame.utils.CHEmpty

interface CHPassCodeCapable {
    fun sendKeyBoardPassCodeDataGetCmd(result: CHResult<CHEmpty>)
    fun keyBoardPassCodeChange(ID: String, hexName: String, result: CHResult<CHEmpty>)
    fun keyBoardPassCodeAdd(id: ByteArray, hexName: String, result: CHResult<CHEmpty>)
    fun keyBoardPassCodeBatchAdd(data: ByteArray, progressCallback: ((current: Int, total: Int) -> Unit)? = null, result: CHResult<CHEmpty>)
    fun keyBoardPassCodeDelete(keyBoardPassCodeID: String, deviceId: String, result: CHResult<CHEmpty>)
    fun keyBoardPassCodeMove(keyBoardPassCodeID: String, touchProUUID: String, result: CHResult<CHEmpty>)
    fun keyBoardPassCodeModeGet(result: CHResult<Byte>)
    fun keyBoardPassCodeModeSet(mode: Byte, result: CHResult<CHEmpty>)
    fun getBoardPassCodeDataSyncCapable(): CHDataSynchronizeCapable

    fun registerEventDelegate(device: CHCapabilityHost, delegate: CHPassCodeDelegate)
    fun unregisterEventDelegate(device: CHCapabilityHost,delegate: CHPassCodeDelegate)
}