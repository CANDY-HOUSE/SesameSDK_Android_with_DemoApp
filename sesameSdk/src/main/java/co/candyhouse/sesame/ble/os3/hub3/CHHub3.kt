package co.candyhouse.sesame.ble.os3.hub3

import co.candyhouse.sesame.ble.os3.wm2.CHWifiModule2
import co.candyhouse.sesame.ble.os3.wm2.CHWifiModule2Delegate

import co.candyhouse.sesame.utils.CHResult

interface CHHub3Delegate : CHWifiModule2Delegate {
    fun onWifiScanStarted(device: CHHub3) {}
    fun onWifiScanFinished(device: CHHub3) {}
}

interface CHHub3 : CHWifiModule2 {
    fun <T> isBleAvailable(result: CHResult<T>): Boolean
    val isRelayOn: Boolean
}
