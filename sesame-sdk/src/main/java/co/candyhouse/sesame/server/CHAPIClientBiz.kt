package co.candyhouse.sesame.server

import android.content.Context
import android.util.Base64
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.base.CHSesameLock
import co.candyhouse.sesame.server.dto.AppPromotion
import co.candyhouse.sesame.server.dto.AppPromotionReadRequest
import co.candyhouse.sesame.server.dto.AppPromotionResponse
import co.candyhouse.sesame.server.dto.AuthenticationDataWrapper
import co.candyhouse.sesame.server.dto.BotScriptRequest
import co.candyhouse.sesame.server.dto.CHBatteryDataReq
import co.candyhouse.sesame.server.dto.CHDeviceInfo
import co.candyhouse.sesame.server.dto.CHFcmTokenUpload
import co.candyhouse.sesame.server.dto.CHRemoveSignKeyRequest
import co.candyhouse.sesame.server.dto.CHSS2RegisterReq
import co.candyhouse.sesame.server.dto.CHSS2RegisterRes
import co.candyhouse.sesame.server.dto.CHSS2WebCMDReq
import co.candyhouse.sesame.server.dto.CHSS5HisUploadRequest
import co.candyhouse.sesame.server.dto.CHSSMHisUploadRequest
import co.candyhouse.sesame.server.dto.CHUserKey
import co.candyhouse.sesame.server.dto.FirmwareZipUrlResponse
import co.candyhouse.sesame.server.dto.RedeemQRRequest
import co.candyhouse.sesame.server.dto.ScenePayload
import co.candyhouse.sesame.server.dto.SubscriptionRequest
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
import com.amplifyframework.api.rest.RestOptions
import com.amplifyframework.kotlin.core.Amplify
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * API Gateway 业务
 *
 * @author frey on 2026/1/12
 */
object CHAPIClientBiz {

    private lateinit var appContext: Context

    private val httpScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()

    @Volatile
    private var initialized = false

    @JvmStatic
    @Synchronized
    fun initialize(context: Context) {
        appContext = context.applicationContext
        initialized = true
    }

    private fun requireInit() {
        check(initialized) { "CHAPIClientBiz is not initialized. Call CHAPIClientBiz.initialize(...) first." }
    }

    private fun identifyId(): String {
        requireInit()
        return AppIdentifyIdUtil.get(appContext)
    }

    private fun identifyHeader(): Map<String, String> {
        return mapOf("appidentifyid" to identifyId())
    }

    private fun <T> makeApiCall(onResponse: CHResult<T>, block: suspend () -> T) {
        requireInit()
        httpScope.launch {
            runCatching { block() }
                .onSuccess { onResponse(Result.success(CHResultState.CHResultStateNetworks(it))) }
                .onFailure { onResponse(Result.failure(toApiException(it))) }
        }
    }

    private fun toApiException(error: Throwable): Throwable {
        return if (error is CHApiException) {
            error
        } else {
            CHApiException(0, error.localizedMessage, error)
        }
    }

    private suspend inline fun <reified T> apiGet(
        path: String,
        headers: Map<String, String> = emptyMap(),
        query: Map<String, String> = emptyMap()
    ): T = parse(apiRequest("GET", path, null, headers, query))

    private suspend inline fun <reified T> apiPost(
        path: String,
        body: Any?,
        headers: Map<String, String> = emptyMap()
    ): T = parse(apiRequest("POST", path, body, headers))

    private suspend inline fun <reified T> apiPut(
        path: String,
        body: Any?,
        headers: Map<String, String> = emptyMap()
    ): T = parse(apiRequest("PUT", path, body, headers))

    private suspend inline fun <reified T> apiDelete(
        path: String,
        body: Any?,
        headers: Map<String, String> = emptyMap()
    ): T = parse(apiRequest("DELETE", path, body, headers))

    private suspend fun apiRequest(
        method: String,
        path: String,
        body: Any?,
        headers: Map<String, String>,
        query: Map<String, String> = emptyMap()
    ): String {
        val optionsBuilder = RestOptions.builder()
            .addPath(path)
            .addHeader("x-api-key", co.candyhouse.sesame.BuildConfig.API_GATEWAY_API_KEY)

        headers.forEach { (key, value) -> optionsBuilder.addHeader(key, value) }
        if (query.isNotEmpty()) optionsBuilder.addQueryParameters(query)
        body?.let { optionsBuilder.addBody(gson.toJson(it).toByteArray(Charsets.UTF_8)) }

        val response = when (method) {
            "GET" -> Amplify.API.get(optionsBuilder.build())
            "POST" -> Amplify.API.post(optionsBuilder.build())
            "PUT" -> Amplify.API.put(optionsBuilder.build())
            "DELETE" -> Amplify.API.delete(optionsBuilder.build())
            else -> error("Unsupported API method: $method")
        }

        val statusCode = response.code.hashCode()
        val responseBody = response.data.asString()
        if (!response.code.isSuccessful) {
            throw CHApiException(
                statusCode,
                parseApiErrorMessage(responseBody) ?: "HTTP $statusCode"
            )
        }
        return responseBody
    }

    private fun parseApiErrorMessage(responseBody: String): String? {
        return runCatching {
            val json = gson.fromJson(responseBody, Map::class.java)
            sequenceOf("message", "errorMessage", "error")
                .mapNotNull { key -> json?.get(key)?.toString() }
                .firstOrNull { it.isNotBlank() }
        }.getOrNull()
    }

    private inline fun <reified T> parse(json: String): T {
        if (T::class == Unit::class) return Unit as T
        if (T::class == Any::class) return gson.fromJson(json, Any::class.java) as T
        if (T::class == String::class) {
            return runCatching { gson.fromJson(json, String::class.java) as T }
                .getOrElse { json as T }
        }
        return gson.fromJson(json, T::class.java)
    }

    // 发送網路鑰匙
    fun upLoadKeys(keys: List<CHUserKey>, onResponse: CHResult<Array<CHUserKey>>) =
        makeApiCall(onResponse) { apiPost("/device", keys, identifyHeader()) }

    // 更新網路鑰匙
    fun putKey(key: CHUserKey, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) {
            apiPut<Unit>("/device", key, identifyHeader())
            CHEmpty()
        }

    // 获取網路鑰匙
    fun getDevicesList(onResponse: CHResult<Array<CHUserKey>>) =
        makeApiCall(onResponse) { apiGet("/device/list", identifyHeader()) }

    // 移除用戶網路鑰匙
    fun removeKey(keyId: String, onResponse: CHResult<CHEmpty>) =
        makeApiCall(onResponse) {
            apiDelete<Unit>("/device", keyId, identifyHeader())
            CHEmpty()
        }

    // 新增好友
    fun addFriend(friendID: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) {
            apiPost<Unit>("/friend", friendID, identifyHeader())
            CHEmpty()
        }

    // 上传用户 Token
    fun uploadUserDeviceToken(deviceToken: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) {
            apiPost<Unit>("/friend/token", deviceToken, identifyHeader())
            CHEmpty()
        }

    // 获取网页链接
    fun getWebUrlByScene(scene: String, extInfo: Map<String, String>? = null, onResponse: CHResult<String>) {
        makeApiCall(onResponse) {
            val req = ScenePayload(scene = scene, token = TokenManager.getValidTokenValue(), extInfo = extInfo)
            val resp: Any = apiPost("/web_route", req, identifyHeader())
            val responseJson = gson.toJsonTree(resp).asJsonObject
            responseJson["url"]?.takeUnless { it.isJsonNull }?.asString
                ?: throw CHApiException(
                    0,
                    responseJson["message"]?.takeUnless { it.isJsonNull }?.asString ?: "Empty URL"
                )
        }
    }

    // 移除用户 Token
    fun cancelNotification(device: CHSesameLock, fcmToken: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) {
            apiDelete<Unit>(
                "/device/v1/token",
                CHFcmTokenUpload((device as CHDevices).deviceId.toString().uppercase(), fcmToken)
            )
            CHEmpty()
        }

    // 访客钥匙签名
    internal fun signGuestKey(key: CHRemoveSignKeyRequest, onResponse: CHResult<String>) =
        makeApiCall(onResponse) { apiPost("/device/v1/sesame2/sign", key) }

    // 上传固件版本号
    fun updateDeviceFirmwareVersion(deviceUUID: String, versionTag: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) {
            apiPost<Unit>("/device/v1/sesame5/$deviceUUID/fwVer", mapOf("versionTag" to versionTag))
            CHEmpty()
        }

    // 上传历史记录标签
    fun postSS2History(deviceID: String, hisHex: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) {
            apiPost<Unit>("/device/v1/sesame2/historys", CHSSMHisUploadRequest(deviceID, hisHex))
            CHEmpty()
        }

    // 上传历史记录标签
    fun postOS3History(deviceID: String, hisHex: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) {
            apiPost<Unit>("/device/v1/sesame2/historys", CHSS5HisUploadRequest(deviceID, hisHex, "5"))
            CHEmpty()
        }

    // 发送IoT命令到设备
    internal fun cmdSesame(cmd: SesameItemCode, ss2: CHDevices, historytag: ByteArray, onResponse: CHResult<CHEmpty>) =
        makeApiCall(onResponse) {
            val msg = System.currentTimeMillis().toUInt24ByteArray()
            val keyCheck = AesCmac((ss2 as CHDeviceUtil).sesame2KeyData!!.secretKey.hexStringToByteArray(), 16)
                .computeMac(msg)!!
                .sliceArray(0..3)

            apiPost<Unit>(
                "/device/v1/iot/sesame2/${ss2.deviceId.toString().uppercase()}",
                CHSS2WebCMDReq(cmd.value, historytag.base64Encode(), keyCheck.toHexString())
            )
            CHEmpty()
        }

    // 生物识别数据操作 (通用)
    fun postCredentialListToServer(credentialListRequest: AuthenticationDataWrapper, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) {
            if (credentialListRequest.operation.endsWith("_put")) {
                apiPost<Unit>("/device/v1/biometrics", credentialListRequest)
                CHEmpty()
            } else {
                apiPost("/device/v1/biometrics", credentialListRequest)
            }
        }

    // 生物识别数据操作 (通用)
    fun updateAuthenticationName(authData: Any, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { apiPost("/device/v1/biometrics", authData) }

    // 生物识别数据操作 (通用)
    fun deleteCredentialInfo(request: AuthenticationDataWrapper, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) {
            apiPost<Unit>("/device/v1/biometrics", request)
            CHEmpty()
        }

    // 订阅 SNS 主题
    fun subscribeToTopic(body: SubscriptionRequest, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { apiPost("/device/v1/subscribe", body) }

    /**
     * 兑换扫码：qrToken 为扫到的分享钥匙二维码全文，成功回传服务端下发的新 URL(data)，失败透传异常（与其他 API 一致）。
     */
    fun redeemQR(qrToken: String, onResponse: CHResult<String>) =
        makeApiCall(onResponse) {
            val resp = apiPost<Any>("/device/v1/redeem_qr", RedeemQRRequest(qrToken = qrToken))
            Gson().toJsonTree(resp).asJsonObject.get("data")?.takeIf { !it.isJsonNull }?.asString
                ?.takeIf { it.isNotEmpty() }
                ?: throw Exception("Redeem QR failed")
        }

    // 设备排序（服务端 orderKey）——migrate：为缺 orderKey 的设备按旧规则(rank/名)补键
    fun mergeDeviceOrder(onResponse: CHResult<Any>) =
        makeApiCall(onResponse) {
            apiPost<Any>("/device/v1/reorder", mapOf("op" to "merge"), identifyHeader())
            CHEmpty()
        }

    // 设备排序（服务端 orderKey）——reorder：在 prevKey、nextKey 之间生成 orderKey 只更新该设备，返回新 orderKey
    fun moveDeviceOrder(deviceUUID: String, prevKey: String?, nextKey: String?, onResponse: CHResult<String>) =
        makeApiCall(onResponse) {
            val body = mutableMapOf<String, Any>("op" to "move", "deviceUUID" to deviceUUID)
            prevKey?.let { body["prevKey"] = it }
            nextKey?.let { body["nextKey"] = it }
            val resp = apiPost<Any>("/device/v1/reorder", body, identifyHeader())
            Gson().toJsonTree(resp).asJsonObject.get("orderKey")?.takeIf { !it.isJsonNull }?.asString ?: ""
        }

    // 获取当前推广活动红点
    fun getActivePromotion(onResponse: CHResult<AppPromotion>) =
        makeApiCall(onResponse) {
            parsePromotionResponse(apiGet<Any>("/device/v1/appPromotionReads", identifyHeader(), mapOf("action" to "getActivePromotion")))
        }

    // 标记推广活动已读
    fun markPromotionRead(promotionId: String, targetUrl: String?, onResponse: CHResult<AppPromotion>) =
        makeApiCall(onResponse) {
            parsePromotionResponse(
                apiPost<Any>(
                    "/device/v1/appPromotionReads",
                    AppPromotionReadRequest(
                        promotionId = promotionId,
                        targetUrl = targetUrl
                    ),
                    identifyHeader()
                )
            )
        }

    // 上传电池数据
    fun postBatteryData(deviceID: String, payloadString: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { apiPost("/device/v1/sesame5/$deviceID/battery", CHBatteryDataReq(payloadString)) }

    // 注册设备（os2）
    internal fun myDevicesRegisterSesame2Post(deviceId: String?, req: CHSS2RegisterReq?, onResponse: CHResult<CHSS2RegisterRes>) {
        makeApiCall(onResponse) { apiPost("/device/v1/sesame2/$deviceId", req) }
    }

    // 注册设备（os3）
    fun myDevicesRegisterSesame5Post(deviceId: String?, body: Any?, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            apiPost<Unit>("/device/v1/sesame5/$deviceId", body)
            CHEmpty()
        }
    }

    // 更新Sesame设备信息
    fun postCHDeviceInfo(body: CHDeviceInfo, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            apiPost<Unit>("/device/infor", body)
            CHEmpty()
        }
    }

    // 更新 bot script
    fun updateBotScript(body: BotScriptRequest, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) {
            apiPost<Unit>("/device/v1/bot/script", body)
            CHEmpty()
        }

    // 获取固件zip包地址
    fun getFirmwareZipUrl(productType: Int, deviceId: String, firmwareDir: String = "prod", onResponse: CHResult<FirmwareZipUrlResponse>) =
        makeApiCall(onResponse) {
            apiGet(
                "/device/v1/firmwareZipUrl",
                query = mapOf(
                    "productType" to productType.toString(),
                    "deviceId" to deviceId,
                    "firmwareDir" to firmwareDir
                )
            )
        }

    // 更新 Hub3_LTE 继电器开关状态
    fun updateRelay(historytag: ByteArray?, hub3: CHDevices, onResponse: CHResult<CHEmpty>) =
        makeApiCall(onResponse) {
            val sendMap: MutableMap<String, String> = mutableMapOf()
            val timestamp = (System.currentTimeMillis() / 1000).toInt()
            val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
            buffer.putInt(timestamp)
            val msg = buffer.array().sliceArray(1..3) // 取第1-3字节

            val sign = AesCmac((hub3 as CHDeviceUtil).sesame2KeyData!!.secretKey.hexStringToByteArray(), 16)
                .computeMac(msg)!!.sliceArray(0..3)

            val cmd: Int = SesameItemCode.HUB3_ITEM_CODE_RELAY_SWITCH.value.toInt()
            val hub3DeviceId = hub3.deviceId?.toString()?.uppercase() ?: ""
            val deviceIdBytes = hub3DeviceId.toByteArray(Charsets.UTF_8)
            val open = 0x01 // 保留字节，目前固定为0x01，代表开关操作
            val op = open.toByte() // 保留字节，目前固定为0x01，代表开关操作

            val payloadBytes = ByteArray(sign.size + 1 + deviceIdBytes.size + 2)
            var offset = 0
            System.arraycopy(sign, 0, payloadBytes, offset, sign.size)
            offset += sign.size
            payloadBytes[offset++] = cmd.toByte()
            System.arraycopy(deviceIdBytes, 0, payloadBytes, offset, deviceIdBytes.size)
            offset += deviceIdBytes.size
            payloadBytes[offset] = op
            val payload = Base64.encodeToString(payloadBytes, Base64.NO_WRAP)
            val hub3DeviceIdLastSegment = hub3DeviceId.substringAfterLast('-')

            sendMap["action"] = "biz3OperateIoT"
            sendMap["op"] = "cmd"
            sendMap["payload"] = payload
            sendMap["topic"] = "wm2${hub3DeviceIdLastSegment.uppercase()}cmd"

            apiPost<Unit>("/device/v1/wifi_module/$hub3DeviceId/switch", sendMap)
            CHEmpty()
        }

    private fun parsePromotionResponse(resp: Any): AppPromotion {
        val gson = Gson()
        return gson.fromJson(gson.toJsonTree(resp), AppPromotionResponse::class.java).promotion
            ?: error("Invalid promotion response")
    }
}
