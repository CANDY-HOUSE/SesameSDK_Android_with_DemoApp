package candyhouse.sesameos.ir.base.irHttpClientBean

import com.google.gson.annotations.SerializedName

data class IrDeviceRemoteKeyRequest(
    @SerializedName("operation") val operation: String = "emit",
    @SerializedName("hxd") val hxdCommand: String?,
    @SerializedName("learned") val learnedCommand: String = "",
    @SerializedName("irDeviceUUID") val irDeviceUUID: String? = "",
){
    override fun toString(): String {
        return "IrDeviceRemoteKeyRequest(operation='$operation', hxdCommand='$hxdCommand', learnedCommand='$learnedCommand', irDeviceUUID='$irDeviceUUID')"
    }
}