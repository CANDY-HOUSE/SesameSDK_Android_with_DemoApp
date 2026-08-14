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
import co.candyhouse.sesame.utils.TokenManager
import com.amazonaws.services.iot.client.AWSIotMessage
import com.amazonaws.services.iot.client.AWSIotMqttClient
import com.amazonaws.services.iot.client.AWSIotQos
import com.amazonaws.services.iot.client.AWSIotTopic
import com.amazonaws.services.iot.client.auth.Credentials
import com.amazonaws.services.iot.client.auth.CredentialsProvider
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.UUID

private const val CUSTOMER_SPECIFIC_ENDPOINT = BuildConfig.AWS_IOT_ENDPOINT
private const val IOT_KEEP_ALIVE_INTERVAL_MILLISECONDS = 60_000
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

    /** IoT 连接成功回调（用于连接后刷新服务端设备列表） */
    @Volatile
    internal var onReconnected: (() -> Unit)? = null

    private val iotScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connectionJob: Job? = null
    private var reconnectJob: Job? = null
    private var subscribeDevicesJob: Job? = null
    private val subscribedIotDeviceIds = Collections.synchronizedSet(mutableSetOf<String>())
    private val subscribedIotTopics = Collections.synchronizedSet(mutableSetOf<String>())

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

    @Synchronized
    fun reconnectImmediatelyIfWaiting() {
        if (iotStatus != IotStatus.ConnectionLost) return

        reconnectJob?.cancel()
        reconnectJob = iotScope.launch {
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
        if (device.getLevel() == 2) return

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

            val client = createMqttClient(getIotCredentials())
            mqttClient = client
            client.connect()
        } catch (e: Exception) {
            L.e(tag, "IoT连接异常", e)
            scheduleReconnect(mqttClient)
        }
    }

    private fun createMqttClient(credentials: Triple<String, String, String?>): AWSIotMqttClient {
        val (accessKey, secretKey, sessionToken) = credentials
        val credentialsProvider = CredentialsProvider {
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
                // 连接成功：刷新服务端列表（含 stateInfo）
                onReconnected?.invoke()
            }

            override fun onConnectionFailure() {
                if (mqttClient === this) {
                    iotStatus = IotStatus.Reconnecting
                }
                super.onConnectionFailure()
            }

            override fun onConnectionClosed() {
                super.onConnectionClosed()
                if (mqttClient === this) scheduleReconnect(this)
            }
        }.apply {
            // Credentials refresh and client recreation are owned by CHIotManager.
            // This avoids SDK retry tasks silently stopping when credential refresh fails.
            maxConnectionRetries = 0
            keepAliveInterval = IOT_KEEP_ALIVE_INTERVAL_MILLISECONDS
        }
    }

    // 复用 Amplify Auth 的已认证凭证；Amplify 内部自动缓存/到期刷新，无需自管 identityId 与凭证缓存
    private suspend fun getIotCredentials(): Triple<String, String, String?> =
        TokenManager.getCredentials()

    @Synchronized
    private fun scheduleReconnect(client: AWSIotMqttClient?) {
        if (mqttClient !== client) return

        iotStatus = IotStatus.ConnectionLost
        clearIotSubscriptionCache()
        subscribedIotTopics.clear()
        iotScope.launch { resetDevicesOnReconnecting() }
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

    // 连接真正丢失时重置一次设备状态，后续重试不重复清空。
    private suspend fun resetDevicesOnReconnecting() = withContext(Dispatchers.IO) {
        CHDeviceManager.getCandyDevices { result ->
            result.onSuccess { response ->
                if (iotStatus == IotStatus.Connected) return@onSuccess

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

    private fun subscribeTopicInternal(topic: String, onMessage: (ByteArray) -> Unit) {
        if (!subscribedIotTopics.add(topic)) return

        val client = mqttClient
        if (client == null) {
            subscribedIotTopics.remove(topic)
            return
        }

        try {
            client.subscribe(
                object : AWSIotTopic(topic, AWSIotQos.QOS0) {
                    override fun onFailure() {
                        L.e(tag, "IoT订阅失败: $topic")
                    }

                    override fun onTimeout() {
                        L.e(tag, "IoT订阅超时: $topic")
                    }

                    override fun onMessage(message: AWSIotMessage) {
                        onMessage(message.payload)
                    }
                }
            )
        } catch (e: Exception) {
            subscribedIotTopics.remove(topic)
            throw e
        }
    }

}

private fun CHDevices.getLevel(): Int =
    SharedPreferencesUtils.preferences.getInt("l" + deviceId.toString(), -1)
