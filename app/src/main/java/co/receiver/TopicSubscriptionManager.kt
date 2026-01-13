package co.receiver

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit
import co.candyhouse.app.BuildConfig
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.dto.SubscriptionRequest
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.AppIdentifyIdUtil
import co.candyhouse.sesame.utils.SharedPreferencesUtils
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.ConcurrentHashMap

/**
 * 主题订阅
 *
 * @author frey on 2025/5/14
 */
class TopicSubscriptionManager(private val context: Context) {

    private val tag = "TopicSubscriptionManager"

    private val prefs = context.getSharedPreferences("topicSubscription", Context.MODE_PRIVATE)

    private val tokenLocks = ConcurrentHashMap<String, Boolean>()

    // 使用标准主题而非FIFO主题
    private val topics = listOf("app_announcements")

    companion object {
        private const val PREF_APP_VERSION = "last_subscription_app_version"
        private const val SUBSCRIPTION_REFRESH_INTERVAL = 30L * 24 * 60 * 60 * 1000
    }

    fun checkAndSubscribeToTopics() {
        if (shouldRefreshSubscriptions()) {
            L.e(tag, "需要更新订阅...")
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    L.d(tag, "fcmToken:$token")
                    SharedPreferencesUtils.deviceToken = token
                    // 强制刷新所有订阅
                    forceRefreshSubscriptions(token)
                }
            }
        } else {
            L.e(tag, "检查现有订阅...")
            L.d(tag, "androidDeviceId=" + AppIdentifyIdUtil.get(context))
            SharedPreferencesUtils.deviceToken?.let { subscribeToTopicsIfNeeded(it) }
        }
    }

    private fun shouldRefreshSubscriptions(): Boolean {
        val lastSubscriptionTimeKey = "last_subscription_time"

        val lastSubscriptionTime = prefs.getLong(lastSubscriptionTimeKey, 0)
        val lastAppVersion = prefs.getInt(PREF_APP_VERSION, 0)
        val currentTime = System.currentTimeMillis()
        val currentAppVersion = BuildConfig.VERSION_CODE

        return when {
            SharedPreferencesUtils.deviceToken == null -> true
            currentAppVersion > lastAppVersion -> {
                L.d(tag, "检测到版本更新: $lastAppVersion -> $currentAppVersion")
                true
            }

            (currentTime - lastSubscriptionTime > SUBSCRIPTION_REFRESH_INTERVAL) -> {
                L.d(tag, "距离上次订阅超过30天")
                true
            }

            else -> false
        }
    }

    private fun forceRefreshSubscriptions(token: String) {
        clearAllTokenSubscriptions()
        subscribeToTopicsIfNeeded(token)
    }

    private fun clearAllTokenSubscriptions() {
        prefs.edit {
            prefs.all.keys
                .filter { it.startsWith("topic_") }
                .forEach { remove(it) }
        }
        L.d(tag, "已清除所有token的订阅记录，将强制重新订阅")
    }

    private fun subscribeToTopicsIfNeeded(token: String) {
        if (tokenLocks.putIfAbsent(token, true) != null) {
            L.e(tag, "正在订阅中，跳过。Token:$token")
            return
        }

        try {
            topics.forEach { topic ->
                if (!isTopicSubscribed(topic, token)) {
                    L.d(tag, "开始订阅：$topic")
                    subscribeToTopic(topic, token) {
                        checkIfAllTopicsSubscribed(token)
                    }
                } else {
                    L.d(tag, "Token:$token 已经订阅过了")
                }
            }
        } catch (e: Exception) {
            tokenLocks.remove(token)
            throw e
        }
    }

    private fun isTopicSubscribed(topic: String, token: String): Boolean {
        val key = "topic_${topic}_${token.takeLast(10)}"
        return prefs.getBoolean(key, false)
    }

    @SuppressLint("HardwareIds")
    private fun subscribeToTopic(topic: String, token: String, onComplete: (() -> Unit)? = null) {
        // 获取设备唯一标识
        val appIdentifyId = AppIdentifyIdUtil.get(context)
        L.d(tag, "appIdentifyId=$appIdentifyId")

        val request = SubscriptionRequest(
            action = "subscribeToTopic",
            topicName = topic,
            pushToken = token,
            appIdentifyId = appIdentifyId,
            platform = "android"
        )

        CHAPIClientBiz.subscribeToTopic(request) { result ->
            result.onSuccess { response ->
                L.d(tag, "Response data: ${response.data}")

                if (response.data.toString().contains("\"success\":true")) {
                    val key = "topic_${topic}_${token.takeLast(10)}"
                    prefs.edit {
                        putBoolean(key, true)
                        putLong("last_subscription_time", System.currentTimeMillis())
                        putInt(PREF_APP_VERSION, BuildConfig.VERSION_CODE)
                    }
                    L.d(tag, "订阅成功: $topic")
                } else {
                    L.e(tag, "订阅失败: success 不为 true")
                }
            }
            result.onFailure { error ->
                L.e(tag, "Subscribe failed", error)
            }

            onComplete?.invoke()
        }
    }

    private fun checkIfAllTopicsSubscribed(token: String) {
        val allSubscribed = topics.all { isTopicSubscribed(it, token) }
        if (allSubscribed) {
            tokenLocks.remove(token)
            L.d(tag, ">>>> Token:$token 的所有主题订阅完成")
        }
    }

    fun onNewToken(token: String) {
        L.d(tag, "onNewToken…… $token")

        val oldToken = SharedPreferencesUtils.deviceToken
        if (oldToken != token) {
            L.d(tag, "Token已变更，强制刷新订阅")
            SharedPreferencesUtils.deviceToken = token
            forceRefreshSubscriptions(token)
        } else {
            L.d(tag, "Token未变更，跳过处理")
        }
    }

}
