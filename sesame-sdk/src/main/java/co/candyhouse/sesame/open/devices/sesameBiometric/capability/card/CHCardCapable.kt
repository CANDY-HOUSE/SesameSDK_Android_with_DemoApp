package co.candyhouse.sesame.open.devices.sesameBiometric.capability.card

import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHCapabilityHost
import co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale.CHDataSynchronizeCapable
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult

interface CHCardCapable {
    fun sendNfcCardsDataGetCmd(deviceUUID: String, result: CHResult<String>)
    fun cardDelete(cardID: String, result: CHResult<CHEmpty>)
    fun cardMove(cardId: String, touchProUUID: String, result: CHResult<CHEmpty>)
    fun cardAdd(id: ByteArray, hexName: String, result: CHResult<CHEmpty>)
    fun cardBatchAdd(id: ByteArray, progressCallback: ((current: Int, total: Int) -> Unit)? = null, result: CHResult<CHEmpty>)
    fun cardChange(ID: String, hexName: String, result: CHResult<CHEmpty>)
    fun cardChangeValue(ID: String, newID: String, result: CHResult<CHEmpty>)
    fun cardModeGet(result: CHResult<Byte>)
    fun cardModeSet(mode: Byte, result: CHResult<CHEmpty>)
    fun getCardDataSyncCapable(): CHDataSynchronizeCapable

    fun registerEventDelegate(device: CHCapabilityHost, delegate: CHCardDelegate)
    fun unregisterEventDelegate(device: CHCapabilityHost, delegate: CHCardDelegate)
}