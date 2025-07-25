package co.candyhouse.sesame.server.dto

/**
 * 订阅主题
 *
 * @author frey on 2025/5/15
 */
data class SubscriptionRequest(
    val topicName: String,
    val token: String,
    val appDeviceId: String
)
