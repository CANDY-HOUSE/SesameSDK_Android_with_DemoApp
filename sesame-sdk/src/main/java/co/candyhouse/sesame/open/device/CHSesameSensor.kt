package co.candyhouse.sesame.open.device

import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.utils.bytesToShort


interface CHSesameSensorDelegate : CHDeviceStatusDelegate {
    fun onSSM2KeysChanged(device: CHSesameConnector, ssm2keys: Map<String, ByteArray>) {}
}

interface CHSesameSensor : CHSesameLock, CHSesameConnector {
    fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
    fun deleteCards(cardID: String, result: CHResult<CHEmpty>)
    fun getCards(result: CHResult<ArrayList<String>>)
}
