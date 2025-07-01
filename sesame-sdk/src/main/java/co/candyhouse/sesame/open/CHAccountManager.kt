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
import co.candyhouse.sesame.server.dto.CHCardNameRequest
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.server.dto.CHFaceNameRequest
import co.candyhouse.sesame.server.dto.CHFcmTokenUpload
import co.candyhouse.sesame.server.dto.CHFingerPrintNameRequest
import co.candyhouse.sesame.server.dto.CHGuestKey
import co.candyhouse.sesame.server.dto.CHGuestKeyCut
import co.candyhouse.sesame.server.dto.CHHistoryEventV2
import co.candyhouse.sesame.server.dto.CHKeyBoardPassCodeNameRequest
import co.candyhouse.sesame.server.dto.CHModifyGuestKeyRequest
import co.candyhouse.sesame.server.dto.CHPalmNameRequest
import co.candyhouse.sesame.server.dto.CHRemoveGuestKeyRequest
import co.candyhouse.sesame.server.dto.CHRemoveSignKeyRequest
import co.candyhouse.sesame.server.dto.CHSS2Infor
import co.candyhouse.sesame.server.dto.CHSS2WebCMDReq
import co.candyhouse.sesame.server.dto.CHSS5HisUploadRequest
import co.candyhouse.sesame.server.dto.CHSSMHisUploadRequest
import co.candyhouse.sesame.server.dto.SubscriptionRequest
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame.utils.base64Encode
import co.candyhouse.sesame.utils.getClientRegion
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.toHexString
import co.candyhouse.sesame.utils.toUInt24ByteArray
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory
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
    internal var jpAPIclient: CHPrivateAPIClient
    private var httpScope = CoroutineScope(IO)

    init {
        val credentialsProvider = CognitoCachingCredentialsProvider(
            CHBleManager.appContext, CHConfiguration.CLIENT_ID,  // 身份集區 ID
            CHConfiguration.CLIENT_ID!!.getClientRegion() // 區域
        )
        val factory = ApiClientFactory().credentialsProvider(credentialsProvider).apiKey(CHConfiguration.API_KEY).region("ap-northeast-1")

        jpAPIclient = factory.build(CHPrivateAPIClient::class.java) //        L.d("hcia", "網路設定完了:")
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
        makeApiCall(onResponse) {
            val tmpNo = jpAPIclient.fcmTokenGet((device as CHDevices).deviceId.toString().uppercase(), fcmToken)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(tmpNo)))
        }
    }

    internal fun enableNotification(device: CHSesameLock, fcmToken: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val tmpEnable = jpAPIclient.fcmTokenSignPost(CHFcmTokenUpload((device as CHDevices).deviceId.toString().uppercase(), fcmToken))
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(tmpEnable)))
        }
    }

    internal fun cancelNotification(device: CHSesameLock, fcmToken: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val tmpEnable = jpAPIclient.fcmTokenSignDelete(CHFcmTokenUpload((device as CHDevices).deviceId.toString().uppercase(), fcmToken))
//            L.d("hcia", "tmpEnable:" + tmpEnable)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(tmpEnable)))
        }
    }

    internal fun getVersion(onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val version = jpAPIclient.getVersion()

            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(version)))
        }
    }

    internal fun signGuestKey(key: CHRemoveSignKeyRequest, onResponse: CHResult<String>) {
        makeApiCall(onResponse) {
            val guestKeyTag = jpAPIclient.guestKeysSignPost(key)
//            L.d("hcia", "guestKeyTag:" + guestKeyTag)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(guestKeyTag)))
        }
    }


    internal fun generateGuestKey(key: CHGuestKey, onResponse: CHResult<String>) {
        makeApiCall(onResponse) {
            val guestKeyTag = jpAPIclient.guestKeyPost(key.deviceUUID, key)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(guestKeyTag)))
        }
    }

    internal fun getGuestKeys(device: CHDevices, onResponse: CHResult<Array<CHGuestKeyCut>>) {
        makeApiCall(onResponse) {
            val deviceUUID = device.deviceId.toString().uppercase()
            val keyCheck = (AesCmac((device as CHDeviceUtil).sesame2KeyData!!.secretKey.hexStringToByteArray(), 16).computeMac(System.currentTimeMillis().toUInt24ByteArray())!!).sliceArray(0..3)

            val guestKeyTag: Array<CHGuestKeyCut> = jpAPIclient.guestKeysGet(deviceUUID, keyCheck.toHexString())
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(guestKeyTag)))
        }
    }

    internal fun removeGuestKey(removeKeyReq: CHRemoveGuestKeyRequest, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val guestKeyTag = jpAPIclient.guestKeysDelete(removeKeyReq.deviceUUID, removeKeyReq)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(guestKeyTag)))
        }
    }

    internal fun openSensorHistoryGet(deviceUUID: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val res = jpAPIclient.openSensorHistoryGet(deviceUUID)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    internal fun setCardName(cardNameRequest: CHCardNameRequest, onResponse: CHResult<String>) {
        makeApiCall(onResponse) {
            val res = jpAPIclient.setCardName(cardNameRequest)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    internal fun getCardName(cardID: String, cardNameUUID: String, subUUID: String, stpDeviceUUID: String, onResponse: CHResult<String>) {
        makeApiCall(onResponse) {
            val cardName = jpAPIclient.getCardName(cardID, cardNameUUID, subUUID, stpDeviceUUID)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(cardName)))
        }
    }

    internal fun getFingerPrintName(fingerPrintID: String, fingerPrintNameUUID: String, subUUID: String, stpDeviceUUID: String, onResponse: CHResult<String>) {
        makeApiCall(onResponse) {
            val fingerPrintName = jpAPIclient.getFingerPrintName(fingerPrintID, fingerPrintNameUUID, subUUID, stpDeviceUUID)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(fingerPrintName)))
        }
    }

    internal fun setFingerPrintName(fingerPrintNameRequest: CHFingerPrintNameRequest, onResponse: CHResult<String>) {
        makeApiCall(onResponse) {
            val res = jpAPIclient.setFingerPrintName(fingerPrintNameRequest)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    internal fun getFaceName(faceID: String, faceNameUUID: String, subUUID: String, stpDeviceUUID: String, onResponse: CHResult<String>) {
        makeApiCall(onResponse) {
            val faceName = jpAPIclient.getFaceName(faceID, faceNameUUID, subUUID, stpDeviceUUID)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(faceName)))
        }
    }

    internal fun setFaceName(faceNameRequest: CHFaceNameRequest, onResponse: CHResult<String>) {
        makeApiCall(onResponse) {
            val res = jpAPIclient.setFaceName(faceNameRequest)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    internal fun getPalmName(palmID: String, palmNameUUID: String, subUUID: String, stpDeviceUUID: String, onResponse: CHResult<String>) {
        makeApiCall(onResponse) {
            val palmName = jpAPIclient.getPalmName(palmID, palmNameUUID, subUUID, stpDeviceUUID)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(palmName)))
        }
    }

    internal fun setPalmName(palmNameRequest: CHPalmNameRequest, onResponse: CHResult<String>) {
        makeApiCall(onResponse) {
            val res = jpAPIclient.setPalmName(palmNameRequest)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    internal fun getKeyBoardPassCodeName(keyBoardPassCode: String, keyBoardPassCodeNameUUID: String, subUUID: String, stpDeviceUUID: String, onResponse: CHResult<String>) {
        makeApiCall(onResponse) {
            val keyBoardPassCodeName = jpAPIclient.getKeyBoardPassCodeName(keyBoardPassCode, keyBoardPassCodeNameUUID, subUUID, stpDeviceUUID)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(keyBoardPassCodeName)))
        }
    }

    internal fun setKeyBoardPassCodeName(keyBoardPassCodeNameRequest: CHKeyBoardPassCodeNameRequest, onResponse: CHResult<String>) {
        makeApiCall(onResponse) {
            val res = jpAPIclient.setKeyBoardPassCodeName(keyBoardPassCodeNameRequest)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    internal fun getHub3StatusFromIot(deviceUUID: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val res = jpAPIclient.getHub3StatusFromIot(deviceUUID)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    internal fun updateHub3Firmware(deviceUUID: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val res = jpAPIclient.updateHub3Firmware(deviceUUID)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    internal fun changeGuestKeyName(deviceUUID: String, modifyKeyReq: CHModifyGuestKeyRequest, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val guestKeyTag = jpAPIclient.guestKeysMotify(deviceUUID.toUpperCase(), modifyKeyReq)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(guestKeyTag)))
        }
    }


    internal fun postSS2History(devieID: String, hisHex: String, onResponse: CHResult<Any>) {
        L.d("postSSHistory", "2--${devieID}---${hisHex}")
        makeApiCall(onResponse) {
            val chHistoryUploadRes = jpAPIclient.feedHistory(CHSSMHisUploadRequest(devieID, hisHex))
//            L.d("hcia", "chHistoryUploadRes:" + chHistoryUploadRes)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(chHistoryUploadRes)))
        }
    }

    internal fun postSS5History(devieID: String, hisHex: String, onResponse: CHResult<Any>) {
//        try {
//            // 强制抛出一个异常
//            throw Exception("Forced exception")
//        } catch (exception: Exception) {
//            // 在捕获到异常时，调用 onFailure 回调函数
//            onResponse.invoke(Result.failure(exception))
//        }
        makeApiCall(onResponse) {
            L.d("postSSHistory", "5--${devieID}---${hisHex}")
            val chHistoryUploadRes = jpAPIclient.feedHistory(CHSS5HisUploadRequest(devieID, hisHex, "5"))
            L.d("hcia", "[ss5][postSS5History]:" + chHistoryUploadRes)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(chHistoryUploadRes)))
        }
    }

    internal fun getHistory(ss2: CHDevices, cursor: Long?, subUUID: String?, onResponse: CHResult<CHHistoryEventV2>) {
        makeApiCall(onResponse) {
            val ssm2ID = ss2.deviceId.toString().uppercase()
//            L.d("hcia", "(ss2 as CHDevicesUtil).sesame2KeyData!!.secretKey:" + (ss2 as CHDevicesUtil).sesame2KeyData!!.secretKey)
//            L.d("hcia", "(ss2 as CHDevicesUtil).sesame2KeyData!!.secretKey:" + (ss2 as CHDevicesUtil).sesame2KeyData!!.deviceUUID)
//            L.d("hcia", "(ss2 as CHDevicesUtil).sesame2KeyData!!.secretKey:" + (ss2 as CHDevicesUtil).sesame2KeyData!!.deviceUUID.uppercase())

//            L.d("hcia", "ss2.getKey().sesame2PublicKey.length:" + ss2.getKey().sesame2PublicKey.length)
            var checkTagStr: String
            if (ss2.getKey().sesame2PublicKey.length == 8) {
                ///ss5 用公鑰驗證
                checkTagStr = ss2.getKey().sesame2PublicKey
//                L.d("hcia", "sesame2PublicKey :" + ss2.getKey().sesame2PublicKey)
//                L.d("hcia", "checkTagStr:" + checkTagStr)
            } else {
                ///ss4 用私鑰簽名
                val keyCheck = (AesCmac((ss2 as CHDeviceUtil).sesame2KeyData!!.secretKey.hexStringToByteArray(), 16).computeMac(System.currentTimeMillis().toUInt24ByteArray())!!).sliceArray(0..3)
                checkTagStr = keyCheck.toHexString()
            }
            L.d("reqHistory", "res:" + ssm2ID + "---" + cursor + "---" + checkTagStr)
            val res = jpAPIclient.getHistory(ssm2ID, cursor, 15, checkTagStr, subUUID)
            L.d("hcia", "res:" + res)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
        }
    }

    internal fun sdkAuthIOT(ss2: CHDevices, onResponse: CHResult<CHEmpty>) {

        makeApiCall(onResponse) {
            val msg = System.currentTimeMillis().toUInt24ByteArray()
            val keyCheck = (AesCmac((ss2 as CHDeviceUtil).sesame2KeyData!!.secretKey.hexStringToByteArray(), 16).computeMac(msg)!!).sliceArray(0..3)
            jpAPIclient.sdkAuthIOT(ss2.deviceId.toString().uppercase(), keyCheck.toHexString())
            onResponse.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    internal fun cmdSesame(cmd: SesameItemCode, ss2: CHDevices, historytag: ByteArray, onResponse: CHResult<CHEmpty>) {
        makeApiCall(onResponse) {
            val msg = System.currentTimeMillis().toUInt24ByteArray()
            val keyCheck = AesCmac((ss2 as CHDeviceUtil).sesame2KeyData!!.secretKey.hexStringToByteArray(), 16).computeMac(msg)!!.sliceArray(0..3)
            jpAPIclient.ss2CommandToWM2Post(ss2.deviceId.toString().uppercase(), CHSS2WebCMDReq(cmd.value.toByte(), historytag.base64Encode(), keyCheck.toHexString()))
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(CHEmpty())))
        }
    }

    internal fun putSesameInfor(ss2: CHDevices, fw_tag: String, onResponse: CHResult<CHEmpty>) {

        //        if (ss2.getFwVersion() == fw_tag) {
        //            L.d("hcia", "一樣不傳 fw_tag:" + fw_tag)
        //            return
        //        }

        makeApiCall(onResponse) { //            L.d("hcia", " 不一樣傳 fw_tag:" + fw_tag)
            //            L.d("hcia", "🐄 historytag:" + historytag.toHexString())
            val res = jpAPIclient.ss2UploadFwVersion(ss2.deviceId.toString().toUpperCase(), CHSS2Infor(fw_tag)) //            L.d("hcia", "res:" + res)
//            ss2.setFwVersion(fw_tag)
        }
    }

    internal fun postCredentialListToServer(credentialListRequest: AuthenticationDataWrapper, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            try {
                val res = jpAPIclient.postCredentialListToServer(credentialListRequest)
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    internal fun deleteCredentialInfo(request: AuthenticationDataWrapper, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            try {
                val res = jpAPIclient.deleteCredentialInfo(request)
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    fun subscribeToTopic(body: SubscriptionRequest, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val subscribeMesg = jpAPIclient.subscribeToTopic(body)
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(subscribeMesg)))
        }
    }

    internal fun postBatteryData(deviceID: String, payloadString: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            val postBatteryDataRes = jpAPIclient.postBatteryData(deviceID, payloadString)
            L.d("harry", "[ss5][postBatteryData]: $postBatteryDataRes")
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