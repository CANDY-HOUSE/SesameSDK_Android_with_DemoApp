package co.candyhouse.app.tabs.devices.hub3.setting.ir.bean

import com.google.gson.annotations.SerializedName

data class IRCode(
    @SerializedName("keyUUID") val irID: String = "",
    @SerializedName("name") var name: String,
    @SerializedName("uuid") val uuid: String = "",
    var deviceId: String = "",
    @SerializedName("timestamp") val timestamp: Double? = 0.0
) {
    override fun toString(): String {
        return "IRCode(irID='$irID', name='$name', uuid='$uuid', deviceId='$deviceId', timestamp=$timestamp)"
    }
}