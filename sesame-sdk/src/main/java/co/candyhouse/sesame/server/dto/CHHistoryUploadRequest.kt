package co.candyhouse.sesame.server.dto

import com.google.gson.annotations.SerializedName

internal data class CHSSMHisUploadRequest(@SerializedName("s") var device_id: String, @SerializedName("v") var historys: String)
internal data class CHSS5HisUploadRequest(@SerializedName("s") var device_id: String, @SerializedName("v") var historys: String, @SerializedName("t") var type: String)

internal data class CHRemoveSignKeyRequest(var deviceId: String, var token: String, var secretKey: String)

internal data class CHSS2WebCMDReq(var cmd: Byte, var history: String, var sign: String)

internal data class CHFcmTokenUpload(var deviceId: String, var token: String)