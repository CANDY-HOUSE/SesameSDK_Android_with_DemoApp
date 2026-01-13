package co.candyhouse.app.ext.webview.manager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.server.CHAPIClientBiz.getWebUrlByScene

/**
 * WebView URL加载管理
 *
 * @author frey on 2025/11/12
 */
object WebViewUrlLoader {

    /**
     * 通用的URL加载逻辑
     */
    fun loadWebUrl(
        scene: String,
        extInfo: Map<String, String>? = null,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        getWebUrlByScene(scene, extInfo) { result ->
            result.fold(
                onSuccess = { state ->
                    val url = ((state as? CHResultState.CHResultStateNetworks)?.data ?: "") as String
                    if (url.isNotEmpty()) {
                        onSuccess(url)
                    } else {
                        onError("Empty URL")
                    }
                },
                onFailure = { t ->
                    onError(t.message ?: "Load url failed")
                }
            )
        }
    }

    /**
     * Compose中使用的remember版本
     */
    @Composable
    fun rememberWebUrl(
        initialUrl: String,
        scene: String,
        extInfo: Map<String, String> = emptyMap(),
        onError: (String) -> Unit = {}
    ): State<String> {
        val webUrl = remember { mutableStateOf(initialUrl) }

        LaunchedEffect(scene) {
            if (scene.isNotEmpty() && initialUrl.isEmpty()) {
                loadWebUrl(
                    scene = scene,
                    extInfo = extInfo.takeIf { it.isNotEmpty() },
                    onSuccess = { url ->
                        webUrl.value = url
                    },
                    onError = onError
                )
            }
        }

        return webUrl
    }
}