package co.candyhouse.sesame.server.dto

import com.google.gson.annotations.SerializedName
import java.sql.Timestamp

// json data class
data class CHFaceNameRequest(
    @SerializedName("type") val type: Byte,
    @SerializedName("faceNameUUID") val faceNameUUID: String,
    @SerializedName("subUUID") val subUUID: String,
    @SerializedName("stpDeviceUUID") val stpDeviceUUID: String,
    @SerializedName("name") val name: String,
    @SerializedName("faceID") val faceID: String,
    @SerializedName("timestamp") val timestamp: Long = Timestamp(System.currentTimeMillis()).time,
    @SerializedName("op") val op: String
)
