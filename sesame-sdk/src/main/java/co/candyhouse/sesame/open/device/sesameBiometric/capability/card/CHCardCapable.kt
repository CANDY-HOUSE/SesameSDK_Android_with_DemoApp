package co.candyhouse.sesame.open.device.sesameBiometric.capability.card

import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHDataSynchronizeCapable
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.server.dto.CHCardNameRequest
import co.candyhouse.sesame.server.dto.CHEmpty

interface CHCardCapable {
    fun sendNfcCardsDataGetCmd(deviceUUID: String, result: CHResult<String>)
    fun cardDelete(cardID: String, result: CHResult<CHEmpty>)
    fun cardMove(cardId: String, touchProUUID: String, result: CHResult<CHEmpty>)
    fun cardAdd(id: String, name: String, result: CHResult<CHEmpty>)
    fun cardChange(ID: String, name: String, result: CHResult<CHEmpty>)
    fun cardChangeValue(ID: String, newID: String, result: CHResult<CHEmpty>)
    fun cardModeGet(result: CHResult<Byte>)
    fun cardModeSet(mode: Byte, result: CHResult<CHEmpty>)
    fun cardNameGet(cardID: String, cardNameUUID: String, subUUID: String, deviceUUID: String, result: CHResult<String>)
    fun cardNameSet(cardNameRequest: CHCardNameRequest, result: CHResult<String>)
    fun getCardDataSyncCapable(): CHDataSynchronizeCapable

    fun registerEventDelegate(device: CHSesameBiometricBase, delegate: CHCardDelegate)
    fun unregisterEventDelegate(device: CHSesameBiometricBase,delegate: CHCardDelegate)
}