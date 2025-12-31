package co.candyhouse.app.ext.webview.data

import android.os.Bundle
import co.utils.getSerializableCompat

/**
 * WebView 参数配置
 *
 * @author frey on 2025/12/26
 */
data class WebViewConfig(
    val url: String = "",
    val scene: String = "",
    val deviceId: String = "",
    val where: String = "",
    val keyLevel: String = "",
    val title: String = "",
    val pushToken: String = "",
    val params: Map<String, String> = hashMapOf()
) {
    companion object {
        // 从 Bundle arguments 创建
        fun fromArguments(arguments: Bundle?): WebViewConfig {
            return WebViewConfig(
                url = arguments?.getString("url") ?: "",
                scene = arguments?.getString("scene") ?: "",
                deviceId = arguments?.getString("deviceId") ?: "",
                where = arguments?.getString("where") ?: "",
                keyLevel = arguments?.getString("keyLevel") ?: "",
                title = arguments?.getString("title") ?: "",
                pushToken = arguments?.getString("pushToken") ?: "",
                params = arguments?.getSerializableCompat<HashMap<String, String>>("extInfo") ?: hashMapOf()
            )
        }
    }

    // 转换为 Bundle（用于 Navigation 传递）
    fun toBundle(): Bundle {
        return Bundle().apply {
            putString("url", url)
            putString("scene", scene)
            putString("deviceId", deviceId)
            putString("where", where)
            putString("keyLevel", keyLevel)
            putString("title", title)
            putString("pushToken", pushToken)
            putSerializable("extInfo", HashMap(params))
        }
    }

    // 构建完整的 extInfo（用于 URL 加载）
    fun buildExtInfo(): Map<String, String> {
        return buildMap {
            if (pushToken.isNotEmpty()) put("pushToken", pushToken)
            if (deviceId.isNotEmpty()) put("deviceUUID", deviceId)
            if (keyLevel.isNotEmpty()) put("keyLevel", keyLevel)
            putAll(params)
        }
    }
}
