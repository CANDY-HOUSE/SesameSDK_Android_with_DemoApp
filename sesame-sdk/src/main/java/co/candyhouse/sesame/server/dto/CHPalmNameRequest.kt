package co.candyhouse.sesame.server.dto

import com.google.gson.annotations.SerializedName
import java.sql.Timestamp

// json data class
data class CHPalmNameRequest(
    @SerializedName("type") val type: Byte,
    @SerializedName("palmNameUUID") val palmNameUUID: String,
    @SerializedName("subUUID") val subUUID: String,
    @SerializedName("stpDeviceUUID") val stpDeviceUUID: String,
    @SerializedName("name") val name: String,
    @SerializedName("palmID") val palmID: String,
    @SerializedName("timestamp") val timestamp: Long = Timestamp(System.currentTimeMillis()).time,
)
