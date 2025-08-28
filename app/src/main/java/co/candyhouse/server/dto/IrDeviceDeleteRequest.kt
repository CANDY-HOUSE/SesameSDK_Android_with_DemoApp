package co.candyhouse.server.dto

import com.google.gson.annotations.SerializedName

data class IrDeviceDeleteRequest(
    @SerializedName("uuid") val uuid: String
)