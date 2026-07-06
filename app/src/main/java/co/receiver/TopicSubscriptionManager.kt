package co.receiver

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.dto.SubscriptionRequest
import co.candyhouse.sesame.utils.AppIdentifyIdUtil
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.SharedPreferencesUtils
import co.candyhouse.sesame.utils.isInternetAvailable
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.google.gson.JsonParser

/**
 * 主题订阅
 *
 * 对齐 iOS：每次拿到 token / 登录状态变化即直接订阅（SNS 幂等，无节流 / 版本号 / 本地去重），
 * 订阅请求随带 App 环境信息(env)，成功后用服务端返回的 envId 作为 history tag 来源。
 *
 * @author frey on 2025/5/14
 */
class TopicSubscriptionManager(private val context: Context) {

    private val tag = "TopicSubscriptionManager"

    // 使用标准主题而非FIFO主题
    private val topic = "app_announcements"

    private val gson = Gson()

    /** 拉取 token 并订阅（启动 / 登录 / 登出后调用）。 */
    fun checkAndSubscribeToTopics() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                L.d(tag, "fcmToken:$token")
                SharedPreferencesUtils.deviceToken = token
                subscribeToTopic(token)
            } else {
                L.e(tag, "获取 FCM token 失败: ${task.exception}")
            }
        }
    }

    /** FCM token 更新：保存并订阅。 */
    fun onNewToken(token: String) {
        L.d(tag, "onNewToken…… $token")
        SharedPreferencesUtils.deviceToken = token
        subscribeToTopic(token)
    }

    // MARK: - 订阅（每次调用直接订阅，SNS 幂等，无节流 / 去重 / 状态记录）

    @SuppressLint("HardwareIds")
    private fun subscribeToTopic(token: String) {
        // 无网：挂一次性监听，恢复后重试自己
        if (!isInternetAvailable()) {
            L.d(tag, "无网络，挂监听待恢复后重试订阅")
            retryWhenNetworkAvailable { subscribeToTopic(token) }
            return
        }

        val appIdentifyId = AppIdentifyIdUtil.get(context)
        L.d(tag, "appIdentifyId=$appIdentifyId")

        val request = SubscriptionRequest(
            action = "subscribeToTopic",
            topicName = topic,
            pushToken = token,
            appIdentifyId = appIdentifyId,
            platform = "android",
            env = AppEnvironment.collect(context)
        )

        CHAPIClientBiz.subscribeToTopic(request) { result ->
            result.onSuccess { response ->
                L.d(tag, "Response data: ${response.data}")
                val envId = parseEnvId(response.data)
                if (envId != null) {
                    SharedPreferencesUtils.historyEnvId = envId
                    L.d(tag, "订阅成功: $topic envId=$envId")
                } else {
                    L.e(tag, "订阅失败: success 不为 true 或缺少 envId")
                }
            }
            result.onFailure { error ->
                L.e(tag, "Subscribe failed", error)
            }
        }
    }

    /**
     * 解析订阅响应，成功时返回服务端记录主键 envId，否则 null。
     * 兼容 body 为内嵌 JSON 字符串或已展开对象两种形式。
     */
    private fun parseEnvId(data: Any?): String? {
        return runCatching {
            val root = JsonParser.parseString(gson.toJson(data)).asJsonObject
            val body = when {
                root.has("body") && root.get("body").isJsonPrimitive ->
                    JsonParser.parseString(root.get("body").asString).asJsonObject
                else -> root
            }
            val success = body.has("success") && body.get("success").asBoolean
            if (success && body.has("envId")) body.get("envId").asString else null
        }.getOrNull()
    }

    /** 挂一次性网络监听，恢复后回调一次并注销自己。 */
    private fun retryWhenNetworkAvailable(onAvailable: () -> Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runCatching { cm.unregisterNetworkCallback(this) }
                L.d(tag, "网络恢复，重试订阅")
                onAvailable()
            }
        }
        runCatching { cm.registerNetworkCallback(networkRequest, callback) }
    }
}
