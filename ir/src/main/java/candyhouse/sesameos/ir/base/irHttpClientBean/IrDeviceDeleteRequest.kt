package candyhouse.sesameos.ir.base.irHttpClientBean

import com.google.gson.annotations.SerializedName

data class IrDeviceDeleteRequest(
    @SerializedName("uuid") val uuid: String
)