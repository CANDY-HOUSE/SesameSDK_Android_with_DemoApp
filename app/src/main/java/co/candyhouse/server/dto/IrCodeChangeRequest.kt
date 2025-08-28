package co.candyhouse.server.dto

import com.google.gson.annotations.SerializedName

// 请求体数据类
data class IrCodeChangeRequest(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("keyUUID") val keyUUID: String,
    @SerializedName("name") val name: String
)