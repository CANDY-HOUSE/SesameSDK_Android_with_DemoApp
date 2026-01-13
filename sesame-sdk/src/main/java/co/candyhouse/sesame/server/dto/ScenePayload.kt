package co.candyhouse.sesame.server.dto

/**
 *
 * 获取URL
 * @author frey on 2026/1/13
 */
data class ScenePayload(
    val scene: String,
    val token: String? = null,
    val extInfo: Map<String, String>? = null
)
