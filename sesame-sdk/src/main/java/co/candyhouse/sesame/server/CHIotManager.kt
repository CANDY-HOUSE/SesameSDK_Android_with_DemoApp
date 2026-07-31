package co.candyhouse.sesame.server

import co.candyhouse.sesame.BuildConfig
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.os3.CHHub3Device
import co.candyhouse.sesame.ble.os3.CHWifiModule2Device
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.devices.CHWifiModule2NetWorkStatus
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.server.dto.Sesame2Shadow
import co.candyhouse.sesame.server.dto.Sesame5ShadowDocuments
import co.candyhouse.sesame.server.dto.WM2Shadow
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.SharedPreferencesUtils
import com.amazonaws.services.iot.client.AWSIotMessage
import com.amazonaws.services.iot.client.AWSIotMqttClient
import com.amazonaws.services.iot.client.AWSIotQos
import com.amazonaws.services.iot.client.AWSIotTopic
import com.amazonaws.services.iot.client.auth.Credentials
import com.amazonaws.services.iot.client.auth.CredentialsProvider
import com.google.gson.Gson
import aws.sdk.kotlin.services.cognitoidentity.CognitoIdentityClient
import aws.sdk.kotlin.services.cognitoidentity.model.GetCredentialsForIdentityRequest
import aws.sdk.kotlin.services.cognitoidentity.model.GetIdRequest
import aws.sdk.kotlin.services.cognitoidentity.model.NotAuthorizedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.Locale.getDefault
import java.util.UUID

private const val CUSTOMER_SPECIFIC_ENDPOINT = BuildConfig.AWS_IOT_ENDPOINT
private const val CREDENTIAL_REFRESH_WINDOW_SECONDS = 300
private val IOT_IDENTITY_ID_KEY =
    "iot_unauthenticated_identity_id_${BuildConfig.AWS_IDENTITY_POOL_ID}"
private val IDENTITY_POOL_REGION = BuildConfig.AWS_IDENTITY_POOL_ID.substringBefore(":")
private val CUSTOMER_REGION = CUSTOMER_SPECIFIC_ENDPOINT
    .substringAfter(".iot.", IDENTITY_POOL_REGION)
    .substringBefore(".amazonaws.com")

internal object CHIotManager {

    private enum class IotStatus {
        Connected,
        Reconnecting,
        ConnectionLost
    }

    private val tag = "AWSIotMqttManager"

    @Volatile
    private var mqttClient: AWSIotMqttClient? = null

    @Volatile
    private var iotStatus = IotStatus.ConnectionLost
    private val iotScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connectionJob: Job? = null
    private var reconnectJob: Job? = null
    private var subscribeDevicesJob: Job? = null
    private val subscribedIotDeviceIds = Collections.synchronizedSet(mutableSetOf<String>())
    private val credentialsMutex = Mutex()
    private var cachedIotCredentials: CachedIotCredentials? = null

    // 启动连接（从应用启动处调用）
    fun startConnection() {
        // 取消之前的任务
        connectionJob?.cancel()
        reconnectJob?.cancel()

        // 在IO线程执行连接
        connectionJob = iotScope.launch {
            connectIoT()
        }
    }

    fun clearIotSubscriptionCache() {
        subscribeDevicesJob?.cancel()
        subscribeDevicesJob = null
        subscribedIotDeviceIds.clear()
    }

    fun subscribeDevicesIfConnected(devices: List<CHDevices>) {
        if (iotStatus != IotStatus.Connected) {
            return
        }

        subscribeDevicesJob?.cancel()
        subscribeDevicesJob = iotScope.launch {
            devices.forEach { device ->
                if (!isActive) return@launch
                subscribeDeviceIfNeeded(device)
            }
        }
    }

    private fun subscribeDeviceIfNeeded(device: CHDevices) {
        if (iotStatus != IotStatus.Connected) return

        val deviceUtil = device as? CHDeviceUtil
        if (deviceUtil == null) {
            L.d(tag, "🥝 skip iot subscribe, device is not CHDeviceUtil: ${device.deviceId}")
            return
        }

        val deviceId = device.deviceId?.toString()?.lowercase() ?: return

        val shouldSubscribe = subscribedIotDeviceIds.add(deviceId)
        if (!shouldSubscribe) {
            return
        }

        try {
            L.d(tag, "🥝 goIOT subscribe: $deviceId")
            deviceUtil.goIOT()
        } catch (e: Exception) {
            subscribedIotDeviceIds.remove(deviceId)
            L.e(tag, "🥝 goIOT subscribe failed: $deviceId", e)
        }
    }

    private suspend fun connectIoT() {
        L.d(tag, "🥝 啟動連線ＩＯＴ--> iotStatus:$iotStatus")
        // 避免重复连接
        if (iotStatus != IotStatus.ConnectionLost) return

        try {
            iotStatus = IotStatus.Reconnecting
            resetDevicesOnReconnecting()

            val client = createMqttClient()
            mqttClient = client
            client.connect()
        } catch (e: Exception) {
            L.e(tag, "IoT连接异常", e)
            mqttClient?.let(::scheduleReconnect)
        }
    }

    private fun createMqttClient(): AWSIotMqttClient {
        val credentialsProvider = CredentialsProvider {
            val (accessKey, secretKey, sessionToken) = runBlocking { getIotCredentials() }
            Credentials(accessKey, secretKey, sessionToken)
        }

        return object : AWSIotMqttClient(
            CUSTOMER_SPECIFIC_ENDPOINT,
            UUID.randomUUID().toString(),
            credentialsProvider,
            CUSTOMER_REGION
        ) {
            override fun onConnectionSuccess() {
                super.onConnectionSuccess()
                if (mqttClient !== this) return

                L.d(tag, "IoT连接成功")
                iotStatus = IotStatus.Connected
                clearIotSubscriptionCache()
                iotScope.launch { updateDevicesOnConnect() }
            }

            override fun onConnectionFailure() {
                if (mqttClient === this) {
                    iotStatus = IotStatus.Reconnecting
                    iotScope.launch { resetDevicesOnReconnecting() }
                }
                super.onConnectionFailure()
            }

            override fun onConnectionClosed() {
                super.onConnectionClosed()
                if (mqttClient === this) scheduleReconnect(this)
            }
        }.apply {
            maxConnectionRetries = 10
            baseRetryDelay = 1_000
            maxRetryDelay = 5_000
        }
    }

    private suspend fun getIotCredentials(): Triple<String, String, String?> =
        credentialsMutex.withLock {
            val now = System.currentTimeMillis() / 1_000
            cachedIotCredentials
                ?.takeIf { it.expirationEpochSeconds - CREDENTIAL_REFRESH_WINDOW_SECONDS > now }
                ?.let { return@withLock Triple(it.accessKey, it.secretKey, it.sessionToken) }

            val client = CognitoIdentityClient {
                region = IDENTITY_POOL_REGION
            }
            try {
                val preferences = SharedPreferencesUtils.preferences
                val identityId = getOrCreateIotIdentityId(client)
                val credentials = try {
                    fetchIotCredentials(client, identityId)
                } catch (_: NotAuthorizedException) {
                    preferences.edit().remove(IOT_IDENTITY_ID_KEY).apply()
                    fetchIotCredentials(
                        client,
                        getOrCreateIotIdentityId(client)
                    )
                }

                val cached = CachedIotCredentials(
                    accessKey = credentials.accessKeyId
                        ?: error("Unauthenticated AWS access key is unavailable"),
                    secretKey = credentials.secretKey
                        ?: error("Unauthenticated AWS secret key is unavailable"),
                    sessionToken = credentials.sessionToken,
                    expirationEpochSeconds = credentials.expiration?.epochSeconds
                        ?: error("Unauthenticated AWS credential expiration is unavailable")
                )
                cachedIotCredentials = cached
                Triple(cached.accessKey, cached.secretKey, cached.sessionToken)
            } finally {
                client.close()
            }
        }

    private suspend fun getOrCreateIotIdentityId(client: CognitoIdentityClient): String {
        val preferences = SharedPreferencesUtils.preferences
        return preferences.getString(IOT_IDENTITY_ID_KEY, null)
            ?: client.getId(
                GetIdRequest {
                    identityPoolId = BuildConfig.AWS_IDENTITY_POOL_ID
                }
            ).identityId?.also {
                preferences.edit().putString(IOT_IDENTITY_ID_KEY, it).apply()
            }
            ?: error("Unauthenticated Cognito identity is unavailable")
    }

    private suspend fun fetchIotCredentials(
        client: CognitoIdentityClient,
        identityId: String
    ) = client.getCredentialsForIdentity(
        GetCredentialsForIdentityRequest {
            this.identityId = identityId
        }
    ).credentials ?: error("Unauthenticated AWS credentials are unavailable")

    private data class CachedIotCredentials(
        val accessKey: String,
        val secretKey: String,
        val sessionToken: String?,
        val expirationEpochSeconds: Long
    )

    @Synchronized
    private fun scheduleReconnect(client: AWSIotMqttClient) {
        if (mqttClient !== client) return

        iotStatus = IotStatus.ConnectionLost
        clearIotSubscriptionCache()
        reconnectJob?.cancel()
        reconnectJob = iotScope.launch {
            delay(5_000)
            if (mqttClient === client && iotStatus == IotStatus.ConnectionLost) {
                connectIoT()
            }
        }
    }

    // 连接成功后更新设备
    private suspend fun updateDevicesOnConnect() = withContext(Dispatchers.IO) {
        CHDeviceManager.getCandyDevices { result ->
            result.onSuccess { response ->
                subscribeDevicesIfConnected(response.data)
            }
        }
    }

    // 重连时重置设备状态
    private suspend fun resetDevicesOnReconnecting() = withContext(Dispatchers.IO) {
        CHDeviceManager.getCandyDevices { result ->
            result.onSuccess { response ->
                response.data.forEach { device ->
                    device.deviceShadowStatus = null

                    when (device) {
                        is CHWifiModule2Device -> {
                            device.mechStatus = CHWifiModule2NetWorkStatus(
                                null, null, null,
                                isAPConnecting = false,
                                isConnectingNet = false,
                                isConnectingIOT = false,
                                isAPCheck = null
                            )
                        }

                        is CHHub3Device -> {
                            device.mechStatus = CHWifiModule2NetWorkStatus(
                                null, null,
                                isIOTWork = false,
                                isAPConnecting = false,
                                isConnectingNet = false,
                                isConnectingIOT = false,
                                isAPCheck = false
                            )
                        }
                    }
                }
            }
        }
    }

    fun subscribeSesame2Shadow(ssm2: CHDevices, onResponse: CHResult<Sesame2Shadow>) {
        L.d(tag, "🐖 請求訂閱 ssm2 iotStatus:" + iotStatus + " " + ssm2.deviceId.toString().uppercase())

        if (iotStatus != IotStatus.Connected) {
            return
        }

        doSubscribeSSM(ssm2, onResponse)
    }

    private fun doSubscribeSSM(ssm2: CHDevices, onResponse: CHResult<Sesame2Shadow>) {
        val ss2Topic = "\$aws/things/sesame2/shadow/name/${ssm2.deviceId.toString().uppercase()}/update/documents"
        if (ssm2.deviceShadowStatus == null) {
            subscribeTopicInternal(ss2Topic) { data ->
                try {
                    L.d(tag, "String(data): " + String(data))
                    val ss5StateIot = Gson().fromJson(String(data), Sesame5ShadowDocuments::class.java)
                    L.d(tag, "ss2StateIot: $ss5StateIot")
                    var ss2StateIot: Sesame2Shadow? = null
                    ss2StateIot = Sesame2Shadow(ss5StateIot.current.state)
                    onResponse.invoke(Result.success(CHResultState.CHResultStateBLE(ss2StateIot)))
                } catch (e: Exception) {
                    L.d("hub3_ss5", "🥝 ssm影子格式不符合e: " + ssm2 + e)
                }
            }
        }

        iotScope.launch {
            requestSesame2Shadow(ssm2, onResponse)
        }
    }

    fun subscribeWifiModule2(wm2: CHWifiModule2Device, onResponse: CHResult<WM2Shadow>) {
        if (iotStatus != IotStatus.Connected) {
            return
        }
        val topic = "\$aws/things/wm2/shadow/name/" + wm2.deviceId.toString().uppercase().substring(24, 36) + "/update/accepted"
        subscribeTopicInternal(topic) { data ->
            try {
                val ss2StateIOT = Gson().fromJson(String(data), WM2Shadow::class.java)
                onResponse.invoke(Result.success(CHResultState.CHResultStateBLE(ss2StateIOT)))
            } catch (e: Exception) {
                L.d(tag, "🥝 wm2影子格式不符合e:" + e)
            }
        }

        iotScope.launch {
            requestWifiModule2Shadow(wm2, onResponse)
        }
    }

    fun subscribeHub3(hub3: CHHub3Device, onResponse: CHResult<String>) {
        if (iotStatus != IotStatus.Connected) {
            return
        }
        val topic = "\$aws/things/wm2/shadow/name/" + hub3.deviceId.toString().uppercase().substring(24, 36) + "/update/accepted"
        subscribeTopicInternal(topic) { data ->
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(String(data))))
        }
    }

    fun subscribeTopic(topic: String, callback: CHResult<ByteArray>) {
        if (iotStatus != IotStatus.Connected) {
            return
        }
        subscribeTopicInternal(topic) { data ->
            callback.invoke(Result.success(CHResultState.CHResultStateNetworks(data)))
        }
    }

    private fun subscribeTopicInternal(topic: String, blocking: Boolean = false, onMessage: (ByteArray) -> Unit) {
        mqttClient?.subscribe(
            object : AWSIotTopic(topic, AWSIotQos.QOS0) {
                override fun onMessage(message: AWSIotMessage) {
                    onMessage(message.payload)
                }
            },
            blocking
        )
    }

    private fun publishString(topic: String, payload: String = "{}") {
        mqttClient?.publish(topic, AWSIotQos.QOS0, payload)
    }

    private fun requestSesame2Shadow(ssm2: CHDevices, onResponse: CHResult<Sesame2Shadow>) {
        val shadowName = ssm2.deviceId.toString().uppercase()
        val acceptedTopic = "\$aws/things/sesame2/shadow/name/$shadowName/get/accepted"
        subscribeTopicInternal(acceptedTopic, blocking = true) { data ->
            try {
                L.d(tag, "🐖 ss2ShadowGet:" + String(data))
                val ss2StateHttp = Gson().fromJson(String(data), Sesame2Shadow::class.java)
                onResponse.invoke(Result.success(CHResultState.CHResultStateBLE(ss2StateHttp)))
            } catch (e: Exception) {
                L.d(tag, "🐖 ssm影子:" + e.localizedMessage)
            }
        }
        publishString("\$aws/things/sesame2/shadow/name/$shadowName/get")
    }

    private fun requestWifiModule2Shadow(wm2: CHWifiModule2Device, onResponse: CHResult<WM2Shadow>) {
        val shadowName = wm2.deviceId.toString().uppercase(getDefault()).substring(24, 36)
        val acceptedTopic = "\$aws/things/wm2/shadow/name/$shadowName/get/accepted"
        subscribeTopicInternal(acceptedTopic, blocking = true) { data ->
            try {
                L.d(tag, "🐖 wm2ShadowGet:" + String(data))
                val wm2StateHttp = Gson().fromJson(String(data), WM2Shadow::class.java)
                onResponse.invoke(Result.success(CHResultState.CHResultStateBLE(wm2StateHttp)))
            } catch (e: Exception) {
                L.d(tag, "🐖 wm2影子沒創建例外!!:" + e)
            }
        }
        publishString("\$aws/things/wm2/shadow/name/$shadowName/get")
    }

}
