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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.concurrent.CopyOnWriteArraySet
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

private const val CUSTOMER_SPECIFIC_ENDPOINT = BuildConfig.AWS_IOT_ENDPOINT
private const val IOT_KEEP_ALIVE_INTERVAL_MILLISECONDS = 60_000
private const val MAX_TOPICS_PER_CONNECTION = 50
private const val RECONNECT_DELAY_MILLISECONDS = 5_000L
private const val SUBSCRIPTION_RETRY_DELAY_MILLISECONDS = 5_000L
private const val POOL_REFRESH_DEBOUNCE_MILLISECONDS = 200L
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

    private class ConnectionSlot(
        val id: Int,
        val generation: Long
    ) {
        var client: AWSIotMqttClient? = null
        var status = IotStatus.ConnectionLost
        var currentConnectionEstablished = false
        var connectionJob: Job? = null
        var reconnectJob: Job? = null
        val subscriptionMutex = Mutex()
        val assignedTopics = linkedSetOf<String>()
        val subscribedTopics = mutableSetOf<String>()
        val subscribingTopics = mutableSetOf<String>()
    }

    private val tag = "AWSIotMqttManager"

    @Volatile
    private var iotStatus = IotStatus.ConnectionLost

    /** IoT 重连成功监听器。 */
    private val reconnectedListeners = CopyOnWriteArraySet<() -> Unit>()

    internal fun addOnReconnectedListener(listener: () -> Unit) {
        reconnectedListeners.add(listener)
    }

    internal fun removeOnReconnectedListener(listener: () -> Unit) {
        reconnectedListeners.remove(listener)
    }

    private val iotScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val poolLock = Any()
    private val connectionSlots = mutableListOf<ConnectionSlot>()
    private val topicCallbacks = mutableMapOf<String, (ByteArray) -> Unit>()
    private val topicSlots = mutableMapOf<String, ConnectionSlot>()
    private val topicOwners = mutableMapOf<String, String>()
    private val deviceTopics = mutableMapOf<String, MutableSet<String>>()
    private var poolGeneration = 0L
    private var nextSlotId = 1
    private var poolStarted = false
    private var refreshOnNextConnection = true
    private var devicesResetForCurrentOutage = false
    private var refreshJob: Job? = null
    private var subscribeDevicesJob: Job? = null
    private val subscribedIotDeviceIds = Collections.synchronizedSet(mutableSetOf<String>())

    // 启动连接（从应用启动处调用）
    fun startConnection() {
        val slotsToConnect = synchronized(poolLock) {
            poolStarted = true
            if (connectionSlots.isEmpty()) {
                connectionSlots.add(createConnectionSlotLocked())
            }
            connectionSlots.filter { it.status == IotStatus.ConnectionLost }
        }

        slotsToConnect.forEach(::connectSlot)
    }

    fun reconnectImmediatelyIfWaiting() {
        val slotsToConnect = synchronized(poolLock) {
            if (!poolStarted) return

            connectionSlots
                .filter { it.status == IotStatus.ConnectionLost }
                .onEach {
                    it.reconnectJob?.cancel()
                    it.reconnectJob = null
                }
        }

        slotsToConnect.forEach(::connectSlot)
    }

    fun stopConnectionPool() {
        val clients = synchronized(poolLock) {
            poolStarted = false
            poolGeneration++
            iotStatus = IotStatus.ConnectionLost
            refreshOnNextConnection = true
            devicesResetForCurrentOutage = false

            subscribeDevicesJob?.cancel()
            subscribeDevicesJob = null
            refreshJob?.cancel()
            refreshJob = null
            subscribedIotDeviceIds.clear()

            connectionSlots.forEach { slot ->
                slot.connectionJob?.cancel()
                slot.reconnectJob?.cancel()
            }

            val activeClients = connectionSlots.mapNotNull { it.client }
            connectionSlots.clear()
            topicCallbacks.clear()
            topicSlots.clear()
            topicOwners.clear()
            deviceTopics.clear()
            activeClients
        }

        clients.forEach { client ->
            runCatching { client.disconnect(3_000, false) }
                .onFailure { L.e(tag, "IoT连接池关闭异常", it) }
        }
    }

    fun restartConnectionPool() {
        stopConnectionPool()
        startConnection()
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

    private fun createConnectionSlotLocked(): ConnectionSlot =
        ConnectionSlot(nextSlotId++, poolGeneration)

    private fun connectSlot(slot: ConnectionSlot) {
        val connectionJob = synchronized(poolLock) {
            if (!isActiveSlotLocked(slot) || slot.status != IotStatus.ConnectionLost) return

            slot.reconnectJob?.cancel()
            slot.reconnectJob = null
            slot.status = IotStatus.Reconnecting
            updatePoolStatusLocked()

            iotScope.launch(start = CoroutineStart.LAZY) {
                connectSlotInternal(slot)
            }.also { slot.connectionJob = it }
        }

        connectionJob.start()
    }

    private suspend fun connectSlotInternal(slot: ConnectionSlot) {
        var client: AWSIotMqttClient? = null
        try {
            val credentials = getIotCredentials()
            client = createMqttClient(slot, credentials)

            val shouldConnect = synchronized(poolLock) {
                if (!isActiveSlotLocked(slot) || slot.status != IotStatus.Reconnecting) {
                    false
                } else {
                    slot.client = client
                    true
                }
            }
            if (!shouldConnect) return

            client.connect()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val isActiveAttempt = synchronized(poolLock) { isActiveSlotLocked(slot) }
            if (!isActiveAttempt) return
            L.e(tag, "IoT连接异常 slot=${slot.id}", e)
            scheduleReconnect(slot, client)
        }
    }

    private fun createMqttClient(
        slot: ConnectionSlot,
        credentials: Triple<String, String, String?>
    ): AWSIotMqttClient {
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
                handleConnectionSuccess(slot, this)
            }

            override fun onConnectionFailure() {
                super.onConnectionFailure()
                handleConnectionFailure(slot, this)
            }

            override fun onConnectionClosed() {
                super.onConnectionClosed()
                scheduleReconnect(slot, this)
            }
        }.apply {
            // Credentials refresh and client recreation are owned by CHIotManager.
            // This avoids SDK retry tasks silently stopping when credential refresh fails.
            maxConnectionRetries = 0
            keepAliveInterval = IOT_KEEP_ALIVE_INTERVAL_MILLISECONDS
            setCleanSession(true)
        }
    }

    // 复用 Amplify Auth 的已认证凭证；Amplify 内部自动缓存/到期刷新，无需自管 identityId 与凭证缓存
    private suspend fun getIotCredentials(): Triple<String, String, String?> =
        TokenManager.getCredentials()

    private fun handleConnectionSuccess(slot: ConnectionSlot, client: AWSIotMqttClient) {
        val shouldRefresh = synchronized(poolLock) {
            if (!isCurrentClientLocked(slot, client)) return

            slot.status = IotStatus.Connected
            slot.currentConnectionEstablished = true
            slot.connectionJob = null
            slot.reconnectJob?.cancel()
            slot.reconnectJob = null
            slot.subscribedTopics.clear()
            slot.subscribingTopics.clear()
            devicesResetForCurrentOutage = false
            updatePoolStatusLocked()

            refreshOnNextConnection.also { refreshOnNextConnection = false }
        }

        L.d(tag, "IoT连接成功 slot=${slot.id}")
        iotScope.launch { subscribeAssignedTopics(slot) }
        if (shouldRefresh) schedulePoolRefresh()
    }

    private fun handleConnectionFailure(slot: ConnectionSlot, client: AWSIotMqttClient) {
        synchronized(poolLock) {
            if (!isCurrentClientLocked(slot, client)) return

            slot.status = IotStatus.Reconnecting
            updatePoolStatusLocked()
        }
    }

    private fun scheduleReconnect(slot: ConnectionSlot, client: AWSIotMqttClient?) {
        val shouldResetDevices: Boolean
        val reconnectJob: Job

        synchronized(poolLock) {
            if (!isActiveSlotLocked(slot) || slot.client !== client) return
            if (slot.status == IotStatus.ConnectionLost && slot.reconnectJob?.isActive == true) return

            val connectionWasEstablished = slot.currentConnectionEstablished
            slot.status = IotStatus.ConnectionLost
            slot.currentConnectionEstablished = false
            slot.client = null
            slot.connectionJob = null
            slot.subscribedTopics.clear()
            slot.subscribingTopics.clear()
            if (connectionWasEstablished) {
                refreshOnNextConnection = true
            }
            shouldResetDevices = connectionWasEstablished &&
                !hasConnectedSlotLocked() &&
                !devicesResetForCurrentOutage

            if (shouldResetDevices) {
                devicesResetForCurrentOutage = true
                subscribedIotDeviceIds.clear()
            }
            updatePoolStatusLocked()

            reconnectJob = iotScope.launch(start = CoroutineStart.LAZY) {
                delay(RECONNECT_DELAY_MILLISECONDS)
                val shouldReconnect = synchronized(poolLock) {
                    if (!isActiveSlotLocked(slot) || slot.status != IotStatus.ConnectionLost) {
                        false
                    } else {
                        slot.reconnectJob = null
                        true
                    }
                }
                if (shouldReconnect) connectSlot(slot)
            }
            slot.reconnectJob?.cancel()
            slot.reconnectJob = reconnectJob
        }

        reconnectJob.start()
        if (shouldResetDevices) iotScope.launch { resetDevicesOnReconnecting() }
    }

    private fun schedulePoolRefresh() {
        val job = synchronized(poolLock) {
            if (!poolStarted || refreshJob?.isActive == true) return

            val generation = poolGeneration
            iotScope.launch(start = CoroutineStart.LAZY) {
                delay(POOL_REFRESH_DEBOUNCE_MILLISECONDS)
                try {
                    if (!isPoolGenerationActive(generation)) return@launch
                    updateDevicesOnConnect(generation)
                    if (isPoolGenerationActive(generation)) {
                        reconnectedListeners.forEach { listener ->
                            try {
                                listener.invoke()
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                L.e(tag, "IoT reconnect listener failed", error)
                            }
                        }
                    }
                } finally {
                    synchronized(poolLock) {
                        if (poolGeneration == generation) refreshJob = null
                    }
                }
            }.also { refreshJob = it }
        }
        job.start()
    }

    // 连接成功后更新设备
    private suspend fun updateDevicesOnConnect(generation: Long) = withContext(Dispatchers.IO) {
        CHDeviceManager.getCandyDevices callback@{ result ->
            if (!isPoolGenerationActive(generation)) return@callback
            result.onSuccess { response ->
                if (isPoolGenerationActive(generation)) {
                    subscribeDevicesIfConnected(response.data)
                }
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
        subscribeTopicInternal(ssm2.deviceId.toString(), ss2Topic) { data ->
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
        subscribeTopicInternal(wm2.deviceId.toString(), topic) { data ->
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
        subscribeTopicInternal(hub3.deviceId.toString(), topic) { data ->
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(String(data))))
        }
    }

    fun subscribeTopic(device: CHDevices, topic: String, callback: CHResult<ByteArray>) {
        if (iotStatus != IotStatus.Connected) {
            return
        }
        subscribeTopicInternal(device.deviceId.toString(), topic) { data ->
            callback.invoke(Result.success(CHResultState.CHResultStateNetworks(data)))
        }
    }

    fun unsubscribeDevice(deviceId: String) {
        val normalizedDeviceId = deviceId.lowercase()
        val topicsToUnsubscribe = mutableListOf<Triple<ConnectionSlot, AWSIotMqttClient, String>>()

        synchronized(poolLock) {
            subscribedIotDeviceIds.remove(normalizedDeviceId)
            val topics = deviceTopics.remove(normalizedDeviceId).orEmpty().toList()
            topics.forEach { topic ->
                if (topicOwners[topic] != normalizedDeviceId) return@forEach

                topicOwners.remove(topic)
                topicCallbacks.remove(topic)
                val slot = topicSlots.remove(topic) ?: return@forEach
                slot.assignedTopics.remove(topic)
                slot.subscribedTopics.remove(topic)
                slot.subscribingTopics.remove(topic)

                val client = slot.client
                if (client != null && slot.status == IotStatus.Connected) {
                    topicsToUnsubscribe.add(Triple(slot, client, topic))
                }
            }
        }

        topicsToUnsubscribe.forEach { (slot, client, topic) ->
            iotScope.launch { unsubscribeTopic(slot, client, topic) }
        }
    }

    private fun subscribeTopicInternal(
        deviceId: String,
        topic: String,
        onMessage: (ByteArray) -> Unit
    ) {
        var slotToConnect: ConnectionSlot? = null
        var slotToSubscribe: ConnectionSlot? = null
        val normalizedDeviceId = deviceId.lowercase()

        synchronized(poolLock) {
            if (!poolStarted) return

            topicCallbacks[topic] = onMessage
            topicOwners[topic]?.takeIf { it != normalizedDeviceId }?.let { previousOwner ->
                deviceTopics[previousOwner]?.remove(topic)
            }
            topicOwners[topic] = normalizedDeviceId
            deviceTopics.getOrPut(normalizedDeviceId, ::mutableSetOf).add(topic)
            val existingSlot = topicSlots[topic]
            if (existingSlot != null) {
                if (existingSlot.status == IotStatus.Connected) {
                    slotToSubscribe = existingSlot
                }
                return@synchronized
            }

            val slot = connectionSlots.firstOrNull {
                it.status == IotStatus.Connected && it.assignedTopics.size < MAX_TOPICS_PER_CONNECTION
            } ?: connectionSlots.firstOrNull {
                it.assignedTopics.size < MAX_TOPICS_PER_CONNECTION
            } ?: createConnectionSlotLocked().also(connectionSlots::add)

            check(slot.assignedTopics.size < MAX_TOPICS_PER_CONNECTION)
            slot.assignedTopics.add(topic)
            topicSlots[topic] = slot

            when (slot.status) {
                IotStatus.Connected -> slotToSubscribe = slot
                IotStatus.ConnectionLost -> slotToConnect = slot
                IotStatus.Reconnecting -> Unit
            }
        }

        slotToConnect?.let(::connectSlot)
        slotToSubscribe?.let { slot -> iotScope.launch { subscribeTopic(slot, topic) } }
    }

    private suspend fun subscribeAssignedTopics(slot: ConnectionSlot) {
        val topics = synchronized(poolLock) {
            if (!isActiveSlotLocked(slot) || slot.status != IotStatus.Connected) return
            slot.assignedTopics.toList()
        }
        topics.forEach { topic -> subscribeTopic(slot, topic) }
    }

    private suspend fun subscribeTopic(slot: ConnectionSlot, topic: String) {
        slot.subscriptionMutex.withLock {
            val client = synchronized(poolLock) {
                if (!isActiveSlotLocked(slot) ||
                    slot.status != IotStatus.Connected ||
                    topic !in slot.assignedTopics ||
                    topic in slot.subscribedTopics ||
                    !slot.subscribingTopics.add(topic)
                ) {
                    null
                } else {
                    slot.client
                }
            }

            if (client == null) {
                synchronized(poolLock) { slot.subscribingTopics.remove(topic) }
                return@withLock
            }

            val subscriptionFailed = AtomicBoolean(false)
            try {
                client.subscribe(
                    object : AWSIotTopic(topic, AWSIotQos.QOS0) {
                        override fun onFailure() {
                            subscriptionFailed.set(true)
                            handleSubscriptionFailure(slot, client, topic, "失败")
                        }

                        override fun onTimeout() {
                            subscriptionFailed.set(true)
                            handleSubscriptionFailure(slot, client, topic, "超时")
                        }

                        override fun onMessage(message: AWSIotMessage) {
                            dispatchMessage(slot, topic, message.payload)
                        }
                    }
                )

                synchronized(poolLock) {
                    slot.subscribingTopics.remove(topic)
                    if (!subscriptionFailed.get() && isCurrentClientLocked(slot, client)) {
                        if (topic in slot.assignedTopics) {
                            slot.subscribedTopics.add(topic)
                        }
                    }
                }
            } catch (e: Exception) {
                synchronized(poolLock) { slot.subscribingTopics.remove(topic) }
                L.e(tag, "IoT订阅异常 slot=${slot.id}: $topic", e)
                scheduleSubscriptionRetry(slot, client, topic)
            }
        }
    }

    private suspend fun unsubscribeTopic(
        slot: ConnectionSlot,
        client: AWSIotMqttClient,
        topic: String
    ) {
        slot.subscriptionMutex.withLock {
            val shouldUnsubscribe = synchronized(poolLock) {
                isCurrentClientLocked(slot, client) && topic !in slot.assignedTopics
            }
            if (!shouldUnsubscribe) return@withLock

            runCatching { client.unsubscribe(topic) }
                .onFailure { L.e(tag, "IoT取消订阅异常 slot=${slot.id}: $topic", it) }
        }
    }

    private fun handleSubscriptionFailure(
        slot: ConnectionSlot,
        client: AWSIotMqttClient,
        topic: String,
        reason: String
    ) {
        synchronized(poolLock) {
            slot.subscribingTopics.remove(topic)
            slot.subscribedTopics.remove(topic)
        }
        L.e(tag, "IoT订阅$reason slot=${slot.id}: $topic")
        scheduleSubscriptionRetry(slot, client, topic)
    }

    private fun scheduleSubscriptionRetry(
        slot: ConnectionSlot,
        client: AWSIotMqttClient,
        topic: String
    ) {
        iotScope.launch {
            delay(SUBSCRIPTION_RETRY_DELAY_MILLISECONDS)
            val shouldRetry = synchronized(poolLock) {
                isCurrentClientLocked(slot, client) &&
                    slot.status == IotStatus.Connected &&
                    topic in slot.assignedTopics &&
                    topic !in slot.subscribedTopics
            }
            if (shouldRetry) subscribeTopic(slot, topic)
        }
    }

    private fun dispatchMessage(slot: ConnectionSlot, topic: String, payload: ByteArray) {
        val callback = synchronized(poolLock) {
            if (!isActiveSlotLocked(slot) || topicSlots[topic] !== slot) null else topicCallbacks[topic]
        }
        callback?.invoke(payload)
    }

    private fun isActiveSlotLocked(slot: ConnectionSlot): Boolean =
        poolStarted && slot.generation == poolGeneration && connectionSlots.contains(slot)

    private fun isCurrentClientLocked(slot: ConnectionSlot, client: AWSIotMqttClient): Boolean =
        isActiveSlotLocked(slot) && slot.client === client

    private fun isPoolGenerationActive(generation: Long): Boolean = synchronized(poolLock) {
        poolStarted && poolGeneration == generation
    }

    private fun hasConnectedSlotLocked(): Boolean =
        connectionSlots.any { it.status == IotStatus.Connected }

    private fun updatePoolStatusLocked() {
        iotStatus = when {
            hasConnectedSlotLocked() -> IotStatus.Connected
            connectionSlots.any { it.status == IotStatus.Reconnecting } -> IotStatus.Reconnecting
            else -> IotStatus.ConnectionLost
        }
    }

}

private fun CHDevices.getLevel(): Int =
    SharedPreferencesUtils.preferences.getInt("l" + deviceId.toString(), -1)
