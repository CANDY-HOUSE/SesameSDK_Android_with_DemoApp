package co.candyhouse.sesame.open

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.open.device.CHSesameLock
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.server.dto.*
import co.candyhouse.sesame.utils.*
import co.candyhouse.sesame.utils.aescmac.AesCmac
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

typealias HttpResponseCallback<T> = (Result<T>) -> Unit
typealias CHResult<T> = (Result<CHResultState<T>>) -> Unit

sealed class CHResultState<T>(val data: T) {
    open class CHResultStateCache<T>(data: T) : CHResultState<T>(data)
    open class CHResultStateBLE<T>(data: T) : CHResultState<T>(data)
    open class CHResultStateNetworks<T>(data: T) : CHResultState<T>(data)
}

object CHAccountManager {
    private var httpScope = CoroutineScope(IO)


    init {
       L.d("hcia", "網路設定完了:")
    }

    internal fun <T, R, A> T.makeApiCall(onResponse: CHResult<A>, block: T.() -> R) {
        httpScope.launch(EmptyCoroutineContext, CoroutineStart.DEFAULT) {
            runCatching {
                block()
            }.onFailure {
//                L.d("hcia", "APIFail!:" + it)
                onResponse.invoke(Result.failure(it))
            }
        }
    }


    internal fun isNotificationEnable(device: CHSesameLock, fcmToken: String, onResponse: CHResult<Any>) {

    }

    internal fun enableNotification(device: CHSesameLock, fcmToken: String, onResponse: CHResult<Any>) {

    }

    internal fun cancelNotification(device: CHSesameLock, fcmToken: String, onResponse: CHResult<Any>) {

    }


    internal fun signGuestKey(key: CHRemoveSignKeyRequest, onResponse: CHResult<String>) {

    }


    internal fun generateGuestKey(key: CHGuestKey, onResponse: CHResult<String>) {

    }

    internal fun getGuestKeys(device: CHDevices, onResponse: CHResult<Array<CHGuestKeyCut>>) {

    }

    internal fun removeGuestKey(removeKeyReq: CHRemoveGuestKeyRequest, onResponse: CHResult<Any>) {

    }

    internal fun changeGuestKeyName(deviceUUID: String, modifyKeyReq: CHModifyGuestKeyRequest, onResponse: CHResult<Any>) {

    }






    internal fun getHistory(ss2: CHDevices, cursor: Long?, onResponse: CHResult<CHHistoryEventV2>) {

    }

    internal fun sdkAuthIOT(ss2: CHDevices, onResponse: CHResult<CHEmpty>) {

    }

    internal fun cmdSesame(cmd: SesameItemCode, ss2: CHDevices, historytag: ByteArray, onResponse: CHResult<CHEmpty>) {

    }

    internal fun putSesameInfor(ss2: CHDevices, fw_tag: String, onResponse: CHResult<CHEmpty>) {

    }
}

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
