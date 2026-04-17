package co.candyhouse.sesame.open.devices

import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult

interface CHHub3Delegate : CHWifiModule2Delegate {}

interface CHHub3 : CHWifiModule2 {
    fun getHub3StatusFromIot(deviceUUID: String, result: CHResult<Byte>)
    fun <T> isBleAvailable(result: CHResult<T>): Boolean
    fun toggle(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
}