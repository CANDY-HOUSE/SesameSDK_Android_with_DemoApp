package co.candyhouse.server.dto

import com.google.gson.annotations.SerializedName

data class IrDeviceModifyRequest(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("alias") val alias: String
) {
    override fun toString(): String {
        return "IrDeviceModifyRequest(uuid='$uuid', alias='$alias')"
    }
}