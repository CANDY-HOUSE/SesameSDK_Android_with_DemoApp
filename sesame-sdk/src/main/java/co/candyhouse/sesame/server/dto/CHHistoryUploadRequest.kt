package co.candyhouse.sesame.server.dto







internal data class CHRemoveSignKeyRequest(var deviceId: String, var token: String, var secretKey: String)

internal data class CHRemoveGuestKeyRequest(var deviceUUID: String, var guestKeyId: String, var randomTag: String)

internal data class CHModifyGuestKeyRequest(var guestKeyId: String, var keyName: String)

internal data class CHHistoryEvent(val recordID: Int, var keyidx: Long?, val type: Byte, val timeStamp: Long, var historyTag: String?, var devicePk: String?, val parameter:String?)
internal data class CHHistoryEventV2(val histories: Array<CHHistoryEvent>, val cursor: Long?)



internal data class CHGuestKey(var deviceUUID: String//16
    , val deviceModel: String//1
    , val keyIndex: String//2
    , val secretKey: String//16
    , val sesame2PublicKey: String//64
    , var keyName: String)

data class CHGuestKeyCut(var guestKeyId: String, var keyName: String)