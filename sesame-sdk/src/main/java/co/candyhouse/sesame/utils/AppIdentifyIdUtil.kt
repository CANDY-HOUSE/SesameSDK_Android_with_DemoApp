package co.candyhouse.sesame.utils

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.core.content.edit

/**
 * appIdentifyId生成
 *
 * @author frey on 2025/12/23
 */
object AppIdentifyIdUtil {

    private const val PREF_KEY = "appIdentifyId"

    @Volatile
    private var cached: String? = null
    private val lock = Any()

    /**
     * 获取稳定的 AppIdentifyId：
     * 1) 优先返回 SharedPreferences 已保存的值
     * 2) 否则读取 ANDROID_ID（过滤异常值）
     */
    fun get(context: Context): String {
        cached?.let { return it }

        synchronized(lock) {
            cached?.let { return it }

            val sp = SharedPreferencesUtils.preferences

            // 1) SP
            val stored = sp.getString(PREF_KEY, null)
            if (!stored.isNullOrBlank()) {
                cached = stored
                return stored
            }

            // 2) ANDROID_ID
            val appIdentifyId = "ap-northeast-1:" + getAndroidIdOrNull(context)

            sp.edit { putString(PREF_KEY, appIdentifyId) }
            cached = appIdentifyId
            return appIdentifyId
        }
    }

    fun warmUp(context: Context) {
        runCatching { get(context) }
    }

    @SuppressLint("HardwareIds")
    private fun getAndroidIdOrNull(context: Context): String? {
        val raw = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull() ?: return null

        val id = raw.trim()

        return id
            .takeIf { it.isNotBlank() }
            ?.takeIf { it != "9774d56d682e549c" } // 历史坏值
            ?.takeIf { it != "unknown" }
            ?.takeIf { it != "null" }
    }
}