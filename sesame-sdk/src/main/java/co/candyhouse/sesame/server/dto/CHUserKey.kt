package co.candyhouse.sesame.server.dto

/**
 * 设备列表数据
 *
 * @author frey on 2025/8/28
 */
data class CHUserKey(
    var deviceUUID: String,
    val deviceModel: String,
    val keyIndex: String,
    val secretKey: String,
    val sesame2PublicKey: String,
    var deviceName: String?,
    var keyLevel: Int,
    var rank: Int? = null,
    val subUUID: String = "",
    val stateInfo: StateInfo = StateInfo()
)

data class StateInfo(
    val batteryPercentage: Int? = null,
    val CHSesame2Status: String? = null,
    val timestamp: Long? = null,
    val wm2State: Boolean? = null
)