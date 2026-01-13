package co.candyhouse.sesame.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
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
    var result = false
    val connectivityManager = CHBleManager.appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        result = when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    } else {
        connectivityManager.run {
            connectivityManager.activeNetworkInfo?.run {
                result = when (type) {
                    ConnectivityManager.TYPE_WIFI -> true
                    ConnectivityManager.TYPE_MOBILE -> true
                    ConnectivityManager.TYPE_ETHERNET -> true
                    else -> false
                }

            }
        }
    }

    return result
}