package candyhouse.sesameos.ir.base.irHttpClientBean

import com.google.gson.annotations.SerializedName

data class IrCodeDeleteRequest(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("keyUUID") val keyUUID: String
)