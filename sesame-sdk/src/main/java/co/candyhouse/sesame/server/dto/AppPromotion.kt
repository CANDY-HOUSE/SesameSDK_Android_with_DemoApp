package co.candyhouse.sesame.server.dto

/**
 * App推广活动红点
 *
 * @author frey on 2026/7/6
 */
data class AppPromotion(
    val promotionId: String,
    val enabled: Boolean = false,
    val visible: Boolean = false,
    val targetUrl: String = ""
)

data class AppPromotionResponse(
    val promotion: AppPromotion?
)

data class AppPromotionReadRequest(
    val action: String = "markPromotionRead",
    val promotionId: String,
    val platform: String = "android",
    val targetUrl: String? = null
)
