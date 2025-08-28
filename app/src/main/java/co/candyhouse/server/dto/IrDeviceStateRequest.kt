package co.candyhouse.server.dto

import com.google.gson.annotations.SerializedName

data class IrDeviceStateRequest(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("state") val state: String
)