package co.receiver

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.core.content.edit
import co.candyhouse.app.BuildConfig
import co.candyhouse.sesame.open.CHAccountManager
import co.candyhouse.sesame.server.dto.SubscriptionRequest
import co.candyhouse.sesame.utils.L
import co.utils.SharedPreferencesUtils
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
            L.d(tag, "androidDeviceId=" + prefs.getString("androidDeviceId", null))
            SharedPreferencesUtils.deviceToken?.let { subscribeToTopicsIfNeeded(it) }
        }
    }

    private fun shouldRefreshSubscriptions(): Boolean {
        val lastTokenKey = "fcm_token"
        val lastSubscriptionTimeKey = "last_subscription_time"

        val storedToken = prefs.getString(lastTokenKey, null)
        val lastSubscriptionTime = prefs.getLong(lastSubscriptionTimeKey, 0)
        val lastAppVersion = prefs.getInt(PREF_APP_VERSION, 0)
        val currentTime = System.currentTimeMillis()
        val currentAppVersion = BuildConfig.VERSION_CODE

        return when {
            storedToken == null -> true
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
        clearTokenSubscriptions(token)
        subscribeToTopicsIfNeeded(token)
    }

    private fun clearTokenSubscriptions(token: String) {
        prefs.edit {
            val tokenSuffix = token.takeLast(10)
            topics.forEach { topic ->
                val key = "topic_${topic}_${tokenSuffix}"
                remove(key)
            }
        }
        L.d(tag, "已清除token的订阅记录，将强制重新订阅")
    }

    private fun subscribeToTopicsIfNeeded(token: String) {
        if (tokenLocks.putIfAbsent(token, true) != null) {
            L.e(tag, "正在订阅中，跳过。Token:$token")
            return
        }

        prefs.edit(commit = true) { putString("fcm_token", token) }

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
        val androidDeviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        L.d(tag, "androidDeviceId=$androidDeviceId")

        val request = SubscriptionRequest(
            action = "subscribeToTopic",
            topicName = topic,
            token = token,
            appDeviceId = androidDeviceId,
            platform = "android"
        )

        CHAccountManager.subscribeToTopic(request) { result ->
            result.onSuccess { response ->
                L.d(tag, "Response data: ${response.data}")

                if (response.data.toString().contains("\"success\":true")) {
                    val key = "topic_${topic}_${token.takeLast(10)}"
                    prefs.edit {
                        putBoolean(key, true)
                        putLong("last_subscription_time", System.currentTimeMillis())
                        putString("androidDeviceId", androidDeviceId)
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
        val oldToken = prefs.getString("fcm_token", null)
        if (oldToken != null && oldToken != token) {
            L.e(tag, "只清除旧 token 的订阅记录")
            clearOldTokenSubscriptions(oldToken)
        }

        L.d(tag, "onNewToken…… $token")
        subscribeToTopicsIfNeeded(token)
    }

    private fun clearOldTokenSubscriptions(oldToken: String) {
        prefs.edit {
            val tokenSuffix = oldToken.takeLast(10)

            prefs.all.keys
                .filter { it.contains(tokenSuffix) }
                .forEach { remove(it) }

        }
    }
}
