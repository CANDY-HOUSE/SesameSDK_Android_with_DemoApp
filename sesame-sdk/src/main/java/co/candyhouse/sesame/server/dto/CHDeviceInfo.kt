package co.candyhouse.sesame.server.dto

/**
 * 
 *
 * @author frey on 2026/4/29
 */
data class CHDeviceInfo(
    var deviceUUID: String,
    val deviceModel: String,
    val longitude: String,
    val latitude: String,
)
