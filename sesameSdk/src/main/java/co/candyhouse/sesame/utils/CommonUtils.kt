package co.candyhouse.sesame.utils

import co.candyhouse.sesame.ble.CHBleSupport

/**
 * 公共方法和一般类
 *
 * @author frey on 2026/1/13
 */
typealias HttpResponseCallback<T> = (Result<T>) -> Unit
typealias CHResult<T> = (Result<CHResultState<T>>) -> Unit

sealed class CHResultState<T>(val data: T) {
    open class CHResultStateBLE<T>(data: T) : CHResultState<T>(data)
    open class CHResultStateNetworks<T>(data: T) : CHResultState<T>(data)
}

class CHEmpty

fun isInternetAvailable(): Boolean = CHBleSupport.backend.isNetworkAvailable()
