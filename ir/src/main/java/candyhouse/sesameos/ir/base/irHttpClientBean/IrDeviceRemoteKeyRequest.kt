package candyhouse.sesameos.ir.base.irHttpClientBean

import com.google.gson.annotations.SerializedName

data class IrDeviceRemoteKeyRequest(
    @SerializedName("operation") val operation: String = "emit",
    @SerializedName("data") val data: String?,
    @SerializedName("irDeviceUUID") val irDeviceUUID: String? = "",
    @SerializedName("irType") val irType: Int = 0,
){
    override fun toString(): String {
        return "IrDeviceRemoteKeyRequest(operation='$operation', data=$data, irDeviceUUID=$irDeviceUUID, irType=$irType)"
    }
}