package co.candyhouse.sesame.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import co.candyhouse.sesame.open.CHBleManager

/**
 * 公共方法和一般类
 *
 * @author frey on 2026/1/13
 */
typealias HttpResponseCallback<T> = (Result<T>) -> Unit
typealias CHResult<T> = (Result<CHResultState<T>>) -> Unit

sealed class CHResultState<T>(val data: T) {
    open class CHResultStateCache<T>(data: T) : CHResultState<T>(data)
    open class CHResultStateBLE<T>(data: T) : CHResultState<T>(data)
    open class CHResultStateNetworks<T>(data: T) : CHResultState<T>(data)
}

data class LockDeviceState(var state: Int, var position: Float?, var time: Long = 0L)

class CHEmpty

fun isInternetAvailable(): Boolean {
    val connectivityManager = CHBleManager.appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
    return when {
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
    }
}
