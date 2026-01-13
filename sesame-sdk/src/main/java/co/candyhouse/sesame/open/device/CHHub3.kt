package co.candyhouse.sesame.open.device

import co.candyhouse.sesame.utils.CHResult

interface CHHub3Delegate : CHWifiModule2Delegate {}

interface CHHub3 : CHWifiModule2 {
    fun getHub3StatusFromIot(deviceUUID: String, result: CHResult<Byte>)
    fun <T> isBleAvailable(result: CHResult<T>): Boolean
}