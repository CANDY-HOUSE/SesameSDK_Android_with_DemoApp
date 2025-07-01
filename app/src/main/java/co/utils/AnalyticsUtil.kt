package co.utils

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.ktx.Firebase

/**
 * firebase 统计工具
 *
 * @author frey on 2025/7/1
 */
object AnalyticsUtil {

    private val analytics by lazy { Firebase.analytics }

    // 通用事件
    fun logEvent(eventName: String, params: Map<String, Any>? = null) {
        val bundle = Bundle()
        params?.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putBoolean(key, value)
            }
        }
        analytics.logEvent(eventName, bundle)
    }

    // 页面浏览
    fun logScreenView(screenName: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        }
    }

    // 按钮点击
    fun logButtonClick(buttonName: String) {
        logEvent("button_click", mapOf("button_name" to buttonName))
    }
}
