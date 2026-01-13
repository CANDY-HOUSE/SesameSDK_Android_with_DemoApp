package co.candyhouse.sesame.server

import android.content.Context
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHSesameLock
import co.candyhouse.sesame.server.dto.AuthenticationDataWrapper
import co.candyhouse.sesame.server.dto.CHBatteryDataReq
import co.candyhouse.sesame.server.dto.CHFcmTokenUpload
import co.candyhouse.sesame.server.dto.CHRemoveSignKeyRequest
import co.candyhouse.sesame.server.dto.CHSS2RegisterReq
import co.candyhouse.sesame.server.dto.CHSS2RegisterRes
import co.candyhouse.sesame.server.dto.CHSS2WebCMDReq
import co.candyhouse.sesame.server.dto.CHSS5HisUploadRequest
import co.candyhouse.sesame.server.dto.CHSSMHisUploadRequest
import co.candyhouse.sesame.server.dto.CHUserKey
import co.candyhouse.sesame.server.dto.ScenePayload
import co.candyhouse.sesame.server.dto.SubscriptionRequest
import co.candyhouse.sesame.utils.ApiClientConfigBuilder
import co.candyhouse.sesame.utils.AppIdentifyIdUtil
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.utils.TokenManager
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame.utils.base64Encode
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.toHexString
import co.candyhouse.sesame.utils.toUInt24ByteArray
import com.amazonaws.auth.AWSCredentialsProvider
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * API Gateway 业务
 *
 * @author frey on 2026/1/12
 */
object CHAPIClientBiz {

    private lateinit var appContext: Context

    private lateinit var cHApiClient: CHAPIClient

    private val httpScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var initialized = false

    @JvmStatic
    @Synchronized
    fun initialize(
        context: Context,
        credentialsProvider: AWSCredentialsProvider,
        region: String,
        apiKey: String? = null
    ) {
        appContext = context.applicationContext

        val factory = ApiClientConfigBuilder.buildApiClientFactory(
            credentialsProvider = credentialsProvider,
            apiKey = apiKey,
            region = region
        )

        cHApiClient = factory.build(CHAPIClient::class.java)

        initialized = true
    }

    private fun requireInit() {
        check(initialized) { "CHAPIClient is not initialized. Call CHAPIClient.initialize(...) first." }
    }

    private fun identifyId(): String {
        requireInit()
        return AppIdentifyIdUtil.get(appContext)
    }

    private fun <T> makeApiCall(onResponse: CHResult<T>, block: () -> T) {
        requireInit()
        httpScope.launch {
            runCatching { block() }
                .onSuccess { onResponse(Result.success(CHResultState.CHResultStateNetworks(it))) }
                .onFailure { onResponse(Result.failure(it)) }
        }
    }

    fun upLoadKeys(keys: List<CHUserKey>, onResponse: CHResult<Array<CHUserKey>>) =
        makeApiCall(onResponse) { cHApiClient.updateKeys(identifyId(), keys) }

    fun putKey(key: CHUserKey, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.putKey(identifyId(), key) }

    fun getDevicesList(onResponse: CHResult<Array<CHUserKey>>) =
        makeApiCall(onResponse) { cHApiClient.getDevicesList(identifyId()) }

    fun removeKey(keyId: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.removeKey(identifyId(), keyId) }

    fun addFriend(friendID: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.addFriend(identifyId(), friendID) }

    fun uploadUserDeviceToken(deviceToken: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.uploadDeviceToken(identifyId(), deviceToken) }

    fun getWebUrlByScene(scene: String, extInfo: Map<String, String>? = null, onResponse: CHResult<String>) {
        requireInit()
        httpScope.launch {
            TokenManager.getValidToken { result ->
                result.fold(
                    onSuccess = { token ->
                        runCatching {
                            val req = ScenePayload(scene = scene, token = token, extInfo = extInfo)
                            val resp = cHApiClient.getWebUrlByScene(identifyId(), req)
                            val url = Gson().toJsonTree(resp).asJsonObject["url"].asString
                            onResponse(Result.success(CHResultState.CHResultStateNetworks(url)))
                        }.onFailure { onResponse(Result.failure(it)) }
                    },
                    onFailure = { onResponse(Result.failure(it)) }
                )
            }
        }
    }

    fun cancelNotification(device: CHSesameLock, fcmToken: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) {
            cHApiClient.fcmTokenSignDelete(
                CHFcmTokenUpload((device as CHDevices).deviceId.toString().uppercase(), fcmToken)
            )
        }

    internal fun signGuestKey(key: CHRemoveSignKeyRequest, onResponse: CHResult<String>) =
        makeApiCall(onResponse) { cHApiClient.guestKeysSignPost(key) }

    fun getHub3StatusFromIot(deviceUUID: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.getHub3StatusFromIot(deviceUUID) }

    fun updateDeviceFirmwareVersion(deviceUUID: String, versionTag: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) {
            cHApiClient.updateDeviceFirmwareVersion(deviceUUID, mapOf("versionTag" to versionTag))
        }

    fun postSS2History(deviceID: String, hisHex: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.feedHistory(CHSSMHisUploadRequest(deviceID, hisHex)) }

    fun postSS5History(deviceID: String, hisHex: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.feedHistory(CHSS5HisUploadRequest(deviceID, hisHex, "5")) }

    internal fun cmdSesame(cmd: SesameItemCode, ss2: CHDevices, historytag: ByteArray, onResponse: CHResult<CHEmpty>) =
        makeApiCall(onResponse) {
            val msg = System.currentTimeMillis().toUInt24ByteArray()
            val keyCheck = AesCmac((ss2 as CHDeviceUtil).sesame2KeyData!!.secretKey.hexStringToByteArray(), 16)
                .computeMac(msg)!!
                .sliceArray(0..3)

            cHApiClient.ss2CommandToWM2Post(
                ss2.deviceId.toString().uppercase(),
                CHSS2WebCMDReq(cmd.value.toByte(), historytag.base64Encode(), keyCheck.toHexString())
            )
            CHEmpty()
        }

    fun postCredentialListToServer(credentialListRequest: AuthenticationDataWrapper, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.postCredentialListToServer(credentialListRequest) }

    fun updateAuthenticationName(authData: Any, onResponse: CHResult<String>) =
        makeApiCall(onResponse) { cHApiClient.postCredential(authData) }

    fun deleteCredentialInfo(request: AuthenticationDataWrapper, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.deleteCredentialInfo(request) }

    fun subscribeToTopic(body: SubscriptionRequest, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.subscribeToTopic(body) }

    fun postBatteryData(deviceID: String, payloadString: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.postBatteryData(deviceID, CHBatteryDataReq(payloadString)) }

    internal fun myDevicesRegisterSesame2Post(deviceId: String?, req: CHSS2RegisterReq?, onResponse: CHResult<CHSS2RegisterRes>) {
        makeApiCall(onResponse) { cHApiClient.myDevicesRegisterSesame2Post(deviceId, req) }
    }

    fun myDevicesRegisterSesame5Post(deviceId: String?, body: Any?, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) { cHApiClient.myDevicesRegisterSesame5Post(deviceId, body) }
    }
}