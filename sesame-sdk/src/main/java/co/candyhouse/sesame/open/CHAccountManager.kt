package co.candyhouse.sesame.open

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHSesameLock
import co.candyhouse.sesame.server.CHPrivateAPIClient
import co.candyhouse.sesame.server.dto.AuthenticationDataWrapper
import co.candyhouse.sesame.server.dto.CHBatteryDataReq
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.server.dto.CHFcmTokenUpload
import co.candyhouse.sesame.server.dto.CHRemoveSignKeyRequest
import co.candyhouse.sesame.server.dto.CHSS2WebCMDReq
import co.candyhouse.sesame.server.dto.CHSS5HisUploadRequest
import co.candyhouse.sesame.server.dto.CHSSMHisUploadRequest
import co.candyhouse.sesame.server.dto.SubscriptionRequest
import co.candyhouse.sesame.utils.ApiClientConfigBuilder
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame.utils.base64Encode
import co.candyhouse.sesame.utils.getClientRegion
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.toHexString
import co.candyhouse.sesame.utils.toUInt24ByteArray
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
    internal var jpAPIClient: CHPrivateAPIClient
    private var httpScope = CoroutineScope(IO)

    init {
        val credentialsProvider = ApiClientConfigBuilder.createCredentialsProvider(
            appContext = CHBleManager.appContext,
            identityPoolId = CHConfiguration.CLIENT_ID,
            region = CHConfiguration.CLIENT_ID.getClientRegion()
        )
        val factory = ApiClientConfigBuilder.buildApiClientFactory(
            credentialsProvider = credentialsProvider,
            apiKey = CHConfiguration.API_KEY,
            region = "ap-northeast-1"
        )

        jpAPIClient = factory.build(CHPrivateAPIClient::class.java)
    }

    internal fun <T, R, A> T.makeApiCall(onResponse: CHResult<A>, block: T.() -> R) {
        httpScope.launch(EmptyCoroutineContext, CoroutineStart.DEFAULT) {
            runCatching {
                block()
            }.onFailure {
                onResponse.invoke(Result.failure(it))
            }
        }
    }

    internal fun cancelNotification(device: CHSesameLock, fcmToken: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val tmpEnable = jpAPIClient.fcmTokenSignDelete(CHFcmTokenUpload((device as CHDevices).deviceId.toString().uppercase(), fcmToken))
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(tmpEnable)))
        }
    }

    internal fun getVersion(onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val version = jpAPIClient.getVersion()

            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(version)))
        }
    }

    internal fun signGuestKey(key: CHRemoveSignKeyRequest, onResponse: CHResult<String>) {
        makeApiCall(onResponse) {
            val guestKeyTag = jpAPIClient.guestKeysSignPost(key)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(guestKeyTag)))
        }
    }

    internal fun getHub3StatusFromIot(deviceUUID: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.getHub3StatusFromIot(deviceUUID)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    internal fun updateDeviceFirmwareVersion(deviceUUID: String, versionTag: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val body = mapOf("versionTag" to versionTag)
            val res = jpAPIClient.updateDeviceFirmwareVersion(deviceUUID, body)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    internal fun updateHub3Firmware(deviceUUID: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val res = jpAPIClient.updateHub3Firmware(deviceUUID)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    internal fun postSS2History(devieID: String, hisHex: String, onResponse: CHResult<Any>) {
        L.d("postSSHistory", "2--${devieID}---${hisHex}")
        makeApiCall(onResponse) {
            val chHistoryUploadRes = jpAPIClient.feedHistory(CHSSMHisUploadRequest(devieID, hisHex))
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(chHistoryUploadRes)))
        }
    }

    internal fun postSS5History(devieID: String, hisHex: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            L.d("postSSHistory", "5--${devieID}---${hisHex}")
            val chHistoryUploadRes = jpAPIClient.feedHistory(CHSS5HisUploadRequest(devieID, hisHex, "5"))
            L.d("hcia", "[ss5][postSS5History]:$chHistoryUploadRes")
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(chHistoryUploadRes)))
        }
    }

    internal fun cmdSesame(cmd: SesameItemCode, ss2: CHDevices, historytag: ByteArray, onResponse: CHResult<CHEmpty>) {
        makeApiCall(onResponse) {
            val msg = System.currentTimeMillis().toUInt24ByteArray()
            val keyCheck = AesCmac((ss2 as CHDeviceUtil).sesame2KeyData!!.secretKey.hexStringToByteArray(), 16).computeMac(msg)!!.sliceArray(0..3)
            jpAPIClient.ss2CommandToWM2Post(ss2.deviceId.toString().uppercase(), CHSS2WebCMDReq(cmd.value.toByte(), historytag.base64Encode(), keyCheck.toHexString()))
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(CHEmpty())))
        }
    }

    internal fun postCredentialListToServer(credentialListRequest: AuthenticationDataWrapper, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            try {
                val res = jpAPIClient.postCredentialListToServer(credentialListRequest)
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    internal fun updateAuthenticationName(authData: Any, onResponse: CHResult<String>) {
        makeApiCall(onResponse) {
            try {
                val res = jpAPIClient.postCredential(authData)
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    internal fun deleteCredentialInfo(request: AuthenticationDataWrapper, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            try {
                val res = jpAPIClient.deleteCredentialInfo(request)
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    fun subscribeToTopic(body: SubscriptionRequest, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val subscribeMesg = jpAPIClient.subscribeToTopic(body)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(subscribeMesg)))
        }
    }

    internal fun postBatteryData(deviceID: String, payloadString: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val postBatteryDataRes = jpAPIClient.postBatteryData(deviceID, CHBatteryDataReq(payloadString))
            L.d("harry", "[postBatteryData]: $postBatteryDataRes")
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(postBatteryDataRes)))
        }
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