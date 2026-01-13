package co.candyhouse.sesame.server.dto

import co.candyhouse.sesame.db.model.CHDevice

/**
 * User bean
 *
 * @author frey on 2026/1/12
 */
fun cheyKeyToUserKey(key: CHDevice, level: Int, nickName: String, rank: Int? = null): CHUserKey {
    return CHUserKey(
        key.deviceUUID,
        key.deviceModel,
        key.keyIndex,
        key.secretKey,
        key.sesame2PublicKey,
        nickName,
        level,
        rank
    )
}

fun userKeyToCHKey(key: CHUserKey, historyTag: ByteArray? = null): CHDevice {
    val deviceModel = key.deviceModel

    return CHDevice(
        key.deviceUUID,
        deviceModel,
        historyTag,
        key.keyIndex,
        key.secretKey,
        key.sesame2PublicKey
    )
}

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
    val wm2State: Boolean? = null,
    val remoteList: List<IrRemote>? = null
)