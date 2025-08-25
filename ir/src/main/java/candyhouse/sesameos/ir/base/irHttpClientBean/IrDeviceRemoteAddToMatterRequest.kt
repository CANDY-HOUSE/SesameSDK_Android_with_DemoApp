package candyhouse.sesameos.ir.base.irHttpClientBean

import candyhouse.sesameos.ir.ext.IRDeviceType
import com.google.gson.annotations.SerializedName

data class IrDeviceRemoteAddToMatterRequest(
    @SerializedName("irDeviceType") val irDeviceType: Int = IRDeviceType.DEVICE_REMOTE_LIGHT,
    @SerializedName("cmdOn") val cmdOn: String?,
    @SerializedName("cmdOff") val cmdOff: String?,
    @SerializedName("irDeviceUUID") val irDeviceUUID: String? = "",
){
    override fun toString(): String {
        return "IrDeviceRemoteAddToMatterRequest(irDeviceType='$irDeviceType', cmdOn='$cmdOn', cmdOff='$cmdOff', irDeviceUUID='$irDeviceUUID')"
    }
}