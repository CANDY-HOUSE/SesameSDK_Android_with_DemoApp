package co.candyhouse.sesame.server.dto

data class BotScriptRequest(
    val deviceUUID: String,
    val actionIndex: String? = null,
    val alias: String? = null,
    val isDefault: Int? = null,
    val actionData: String? = null,
    val displayOrder: Int? = null,
    val deleteAll: Boolean? = null,
    val batchDisplayOrders: List<BotScriptItem>? = null
)