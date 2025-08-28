package co.candyhouse.server.dto

import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.IRType
import com.google.gson.annotations.SerializedName

data class IrDeviceRemoteAddToMatterRequest(
    @SerializedName("irDeviceType") val irDeviceType: Int = IRType.DEVICE_REMOTE_LIGHT,
    @SerializedName("cmdOn") val cmdOn: String?,
    @SerializedName("cmdOff") val cmdOff: String?,
    @SerializedName("irDeviceUUID") val irDeviceUUID: String? = "",
){
    override fun toString(): String {
        return "IrDeviceRemoteAddToMatterRequest(irDeviceType='$irDeviceType', cmdOn='$cmdOn', cmdOff='$cmdOff', irDeviceUUID='$irDeviceUUID')"
    }
}