package co.candyhouse.sesame.open.device.sesameBiometric.capability.passcode

import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHDataSynchronizeCapable
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.server.dto.CHKeyBoardPassCodeNameRequest

interface CHPassCodeCapable {
    fun sendKeyBoardPassCodeDataGetCmd(result: CHResult<CHEmpty>)
    fun keyBoardPassCodeChange(ID: String, hexName: String, result: CHResult<CHEmpty>)
    fun keyBoardPassCodeAdd(id: ByteArray, hexName: String, result: CHResult<CHEmpty>)
    fun keyBoardPassCodeBatchAdd(data: ByteArray, progressCallback: ((current: Int, total: Int) -> Unit)? = null, result: CHResult<CHEmpty>)
    fun keyBoardPassCodeDelete(keyBoardPassCodeID: String, deviceId: String, result: CHResult<CHEmpty>)
    fun keyBoardPassCodeMove(keyBoardPassCodeID: String, touchProUUID: String, result: CHResult<CHEmpty>)
    fun keyBoardPassCodeModeGet(result: CHResult<Byte>)
    fun keyBoardPassCodeModeSet(mode: Byte, result: CHResult<CHEmpty>)
    fun keyBoardPassCodeNameGet(keyBoardPassCode: String, keyBoardPassCodeNameUUID: String, subUUID: String, deviceUUID: String, result: CHResult<String>)
    fun keyBoardPassCodeNameSet(keyBoardPassCodeNameRequest: CHKeyBoardPassCodeNameRequest, result: CHResult<String>)
    fun getBoardPassCodeDataSyncCapable(): CHDataSynchronizeCapable

    fun registerEventDelegate(device: CHSesameBiometricBase, delegate: CHPassCodeDelegate)
    fun unregisterEventDelegate(device: CHSesameBiometricBase,delegate: CHPassCodeDelegate)
}