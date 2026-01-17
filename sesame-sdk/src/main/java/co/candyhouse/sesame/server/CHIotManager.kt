package co.candyhouse.sesame.server

import co.candyhouse.sesame.BuildConfig
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.os3.CHHub3Device
import co.candyhouse.sesame.ble.os3.CHWifiModule2Device
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHWifiModule2NetWorkStatus
import co.candyhouse.sesame.server.dto.Sesame2Shadow
import co.candyhouse.sesame.server.dto.Sesame5ShadowDocuments
import co.candyhouse.sesame.server.dto.WM2Shadow
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.getClientRegion
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos
import com.amazonaws.mobileconnectors.iot.AWSIotMqttSubscriptionStatusCallback
import com.amazonaws.services.iotdata.AWSIotDataClient
import com.amazonaws.services.iotdata.model.GetThingShadowRequest
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.Locale.getDefault
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val CUSTOMER_SPECIFIC_ENDPOINT = BuildConfig.AWS_IOT_ENDPOINT

internal object CHIotManager {

    private val tag = "AWSIotMqttManager"

    private var mqttManager =
        AWSIotMqttManager(UUID.randomUUID().toString(), CUSTOMER_SPECIFIC_ENDPOINT)
    private var iotDataClient = AWSIotDataClient(createCredentialsProvider()).apply {
        endpoint = CUSTOMER_SPECIFIC_ENDPOINT
    }

    private var iotStatus = AWSIotMqttClientStatus.ConnectionLost
    private var connectionJob: Job? = null

    init {
        L.d(tag, "CHIotManager init:")
        // 初始化配置，不执行连接，避免主线程阻塞造成ANR
        mqttManager.apply {
            setAutoResubscribe(true)
            setReconnectRetryLimits(1, 5)
            maxAutoReconnectAttempts = 10
        }
    }

    // 启动连接（从应用启动处调用）
    fun startConnection() {
        // 取消之前的任务
        connectionJob?.cancel()

        // 在IO线程执行连接
        connectionJob = CoroutineScope(Dispatchers.IO).launch {
            connectIoT()
        }
    }

    private suspend fun connectIoT() {
        L.d(tag, "🥝 啟動連線ＩＯＴ--> iotStatus:$iotStatus")
        // 避免重复连接
        if (iotStatus != AWSIotMqttClientStatus.ConnectionLost) return

        try {
            // 使用协程包装异步回调
            suspendCancellableCoroutine { continuation ->
                val resumed = AtomicBoolean(false)

                mqttManager.connect(createCredentialsProvider()) { status, error ->
                    iotStatus = status
                    L.d(tag, "🥝 IoT连接状态: $status")

                    when (status) {
                        AWSIotMqttClientStatus.Connected -> {
                            // 连接成功，更新设备状态
                            CoroutineScope(Dispatchers.IO).launch {
                                updateDevicesOnConnect()
                            }
                            if (resumed.compareAndSet(false, true) && continuation.isActive) continuation.resume(Unit)
                        }

                        AWSIotMqttClientStatus.Reconnecting -> {
                            // 重连中，重置设备状态
                            CoroutineScope(Dispatchers.IO).launch {
                                resetDevicesOnReconnecting()
                            }
                        }

                        AWSIotMqttClientStatus.ConnectionLost -> {
                            L.d(tag, "🥝 連線狀態 ConnectionLost!!!!!!! IOT:$status")
                            // 连接丢失，延迟重试
                            CoroutineScope(Dispatchers.IO).launch {
                                delay(3000)
                                connectIoT() // 重新连接
                            }
                            if (resumed.compareAndSet(false, true) && continuation.isActive) continuation.resume(Unit)
                        }

                        else -> {
                            if (resumed.compareAndSet(false, true) && continuation.isActive) {
                                if (error != null) continuation.resumeWithException(error)
                                else continuation.resume(Unit)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            L.e(tag, "IoT连接异常", e)
            // 延迟后重试
            delay(5000)
            connectIoT()
        }
    }

    // 连接成功后更新设备
    private suspend fun updateDevicesOnConnect() = withContext(Dispatchers.IO) {
        CHDeviceManager.getCandyDevices { result ->
            result.onSuccess { response ->
                response.data.forEach { device ->
                    (device as? CHDeviceUtil)?.goIOT()
                }
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

    // 创建凭证提供者的辅助方法
    private fun createCredentialsProvider(): CognitoCachingCredentialsProvider {
        return CognitoCachingCredentialsProvider(
            CHBleManager.appContext,
            BuildConfig.API_GATEWAY_CLIENT_ID,
            BuildConfig.API_GATEWAY_CLIENT_ID.getClientRegion()
        )
    }

    fun subscribeSesame2Shadow(ssm2: CHDevices, onResponse: CHResult<Sesame2Shadow>) {
        L.d(tag, "🐖 請求訂閱 ssm2 iotStatus:" + iotStatus + " " + ssm2.deviceId.toString().uppercase())

        if (iotStatus != AWSIotMqttClientStatus.Connected) {
            return
        }

        doSubscribeSSM(ssm2, onResponse)
    }

    private fun doSubscribeSSM(ssm2: CHDevices, onResponse: CHResult<Sesame2Shadow>) {
        val ss2Topic = "\$aws/things/sesame2/shadow/name/${ssm2.deviceId.toString().uppercase()}/update/documents"
        if (ssm2.deviceShadowStatus == null) {
            mqttManager.subscribeToTopic(
                ss2Topic, AWSIotMqttQos.QOS0,
                object : AWSIotMqttSubscriptionStatusCallback {
                    override fun onSuccess() {}

                    override fun onFailure(exception: Throwable?) {
                        L.d("hub3_ss5", "🐖  訂閱失敗 exception")
                    }

                }) { _, data ->
                try {
                    L.d(tag, "String(data!!): " + String(data!!))
                    val ss5StateIot = Gson().fromJson(String(data!!), Sesame5ShadowDocuments::class.java)
                    L.d(tag, "ss2StateIot: $ss5StateIot")
                    var ss2StateIot: Sesame2Shadow? = null
                    ss2StateIot = Sesame2Shadow(ss5StateIot.current.state)
                    onResponse.invoke(Result.success(CHResultState.CHResultStateBLE(ss2StateIot)))
                } catch (e: Exception) {
                    L.d("hub3_ss5", "🥝 ssm影子格式不符合e: " + ssm2 + e)
                }
            }
        }

        try {
            val ss2ShodaowDataHttp = iotDataClient.getThingShadow(
                GetThingShadowRequest().withThingName("sesame2").withShadowName(ssm2.deviceId.toString().uppercase())
            )
            L.d(tag, "🐖 ss2ShodaowDataHttp:" + String(ss2ShodaowDataHttp.payload.array()))
            val ss2StateHttp = Gson().fromJson(String(ss2ShodaowDataHttp.payload.array()), Sesame2Shadow::class.java)
            L.d(tag, "🐖 拿到影子 ss2ShodaowDataHttp:")
            onResponse.invoke(Result.success(CHResultState.CHResultStateBLE(ss2StateHttp)))
        } catch (e: Exception) {
            L.d(tag, "🐖 ssm影子:" + e.localizedMessage)
        }
    }

    fun subscribeWifiModule2(wm2: CHWifiModule2Device, onResponse: CHResult<WM2Shadow>) {
        if (iotStatus != AWSIotMqttClientStatus.Connected) {
            return
        }
        val topic = "\$aws/things/wm2/shadow/name/" + wm2.deviceId.toString().uppercase().substring(24, 36) + "/update/accepted"
        mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0) { topic, data ->
            try {
                val ss2StateIOT = Gson().fromJson(String(data!!), WM2Shadow::class.java)
                onResponse.invoke(Result.success(CHResultState.CHResultStateBLE(ss2StateIOT)))
            } catch (e: Exception) {
                L.d(tag, "🥝 wm2影子格式不符合e:" + e)
            }
        }

        try {
            val wm2ShodaowDataHttp = iotDataClient.getThingShadow(
                GetThingShadowRequest().withThingName("wm2").withShadowName(wm2.deviceId.toString().uppercase(getDefault()).substring(24, 36))
            )
            val wm2StateHttp = Gson().fromJson(String(wm2ShodaowDataHttp.payload.array()), WM2Shadow::class.java)
            onResponse.invoke(Result.success(CHResultState.CHResultStateBLE(wm2StateHttp)))
        } catch (e: Exception) {
            L.d(tag, "🐖 wm2影子沒創建例外!!:" + e)
        }
    }

    fun subscribeHub3(hub3: CHHub3Device, onResponse: CHResult<String>) {
        if (iotStatus != AWSIotMqttClientStatus.Connected) {
            return
        }
        val topic = "\$aws/things/wm2/shadow/name/" + hub3.deviceId.toString().uppercase().substring(24, 36) + "/update/accepted"
        mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0) { topic, data ->
            onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(String(data))))
        }
    }

    fun subscribeTopic(topic: String, callback: CHResult<ByteArray>) {
        if (iotStatus != AWSIotMqttClientStatus.Connected) {
            return
        }
        mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0) { _, data ->
            callback.invoke(Result.success(CHResultState.CHResultStateNetworks(data)))
        }
    }

}