package co.candyhouse.sesame.server.dto

/**
 * 订阅主题
 *
 * @author frey on 2025/5/15
 */
data class SubscriptionRequest(
    val action: String,
    val topicName: String,
    val pushToken: String,
    val appIdentifyId: String,
    val platform: String,
    /** App 环境信息，有序单键对象数组 [{"key": value}, ...]，随订阅上报 */
    val env: List<Map<String, Any>>? = null
)
