package co.candyhouse.sesame.server.dto

import com.google.gson.annotations.SerializedName

internal data class CHSSMHisUploadRequest(@SerializedName("s") var device_id: String, @SerializedName("v") var historys: String)
internal data class CHSS5HisUploadRequest(@SerializedName("s") var device_id: String, @SerializedName("v") var historys: String, @SerializedName("t") var type: String)

internal data class CHFcmTokenUpload(
    var deviceId: String,
    var token: String,
    var subUUID: String = ""
)

internal data class CHRemoveSignKeyRequest(var deviceId: String, var token: String, var secretKey: String)

internal data class CHRemoveGuestKeyRequest(var deviceUUID: String, var guestKeyId: String, var randomTag: String)

internal data class CHModifyGuestKeyRequest(var guestKeyId: String, var keyName: String)

internal data class CHSS2WebCMDReq(var cmd: Byte, var history: String, var sign: String)

internal data class CHSS2Infor(
    var fwv: String,
)

internal data class CHGuestKey(var deviceUUID: String//16
    , val deviceModel: String//1
    , val keyIndex: String//2
    , val secretKey: String//16
    , val sesame2PublicKey: String//64
    , var keyName: String)

data class CHGuestKeyCut(var guestKeyId: String, var keyName: String)