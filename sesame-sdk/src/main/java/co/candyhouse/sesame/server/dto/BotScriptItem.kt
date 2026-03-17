package co.candyhouse.sesame.server.dto

data class BotScriptItem(
    val actionIndex: String,
    val alias: String? = null,
    val displayOrder: Int? = null,
    val isDefault: Int? = null
)