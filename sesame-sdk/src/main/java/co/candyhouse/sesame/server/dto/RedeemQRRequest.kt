package co.candyhouse.sesame.server.dto

/**
 * 兑换扫码请求：qrToken 为扫到的分享钥匙二维码全文
 */
data class RedeemQRRequest(
    val qrToken: String,
    val op: String = "redeemQRToken"
)
