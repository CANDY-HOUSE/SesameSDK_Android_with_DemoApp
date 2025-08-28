package co.candyhouse.server.dto

import com.google.gson.annotations.SerializedName

data class IrLearnedDataAddRequest(
    @SerializedName("IrDataNameUUID") val irDataNameUUID: String,
    @SerializedName("DataLength") val dataLength: Int,
    @SerializedName("Esp32c3LearnedIrDataHexString") val esp32c3LearnedIrDataHexString: String,
    @SerializedName("TimeStamp") val timeStamp: Long,
    @SerializedName("Hub3DeviceUUID") val hub3DeviceUUID: String,
    @SerializedName("irDeviceUUID") val irDeviceUUID: String,
    @SerializedName("keyUUID") val keyUUID: String,
) {
    override fun toString(): String {
        return "IrLearnedDataAddRequest(IrDataNameUUID='$irDataNameUUID', DataLength=${dataLength.toInt()}, Esp32c3LearnedIrDataHexString='$esp32c3LearnedIrDataHexString', TimeStamp=$timeStamp, Hub3DeviceUUID='$hub3DeviceUUID', irDeviceUUID='$irDeviceUUID', keyUUID='$keyUUID')"
    }
}