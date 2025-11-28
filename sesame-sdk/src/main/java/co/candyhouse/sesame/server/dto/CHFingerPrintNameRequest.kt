package co.candyhouse.sesame.server.dto

import com.google.gson.annotations.SerializedName
import java.sql.Timestamp

// json data class
data class CHFingerPrintNameRequest(
    @SerializedName("type") val type: Byte,
    @SerializedName("fingerPrintNameUUID") val fingerPrintNameUUID: String,
    @SerializedName("subUUID") val subUUID: String,
    @SerializedName("stpDeviceUUID") val stpDeviceUUID: String,
    @SerializedName("name") val name: String,
    @SerializedName("fingerPrintID") val fingerPrintID: String,
    @SerializedName("timestamp") val timestamp: Long = Timestamp(System.currentTimeMillis()).time,
    @SerializedName("op") val op: String
)
