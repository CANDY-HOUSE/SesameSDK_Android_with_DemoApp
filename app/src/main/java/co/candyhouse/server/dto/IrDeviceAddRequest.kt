package co.candyhouse.server.dto

import com.google.gson.annotations.SerializedName

data class IrDeviceAddRequest(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("model") val model: String,
    @SerializedName("state") val state: String,
    @SerializedName("alias") val alias: String,
    @SerializedName("type") val type: Int,
    @SerializedName("deviceUUID") val deviceId: String,
    @SerializedName("code") val code: Int,
    @SerializedName("keys") val keys: Array<Map<String,String>> = emptyArray()
){
    override fun toString(): String {
        return "IrDeviceAddRequest(uuid='$uuid', model='$model', state='$state', alias='$alias', type=$type, deviceId='$deviceId', code=$code, keys=${keys.contentToString()})"
    }
}