package co.candyhouse.sesame.server.dto

import com.google.gson.annotations.SerializedName
import java.sql.Timestamp

// json data class
data class CHKeyBoardPassCodeNameRequest(
    @SerializedName("type") val type: Byte,
    @SerializedName("keyBoardPassCodeNameUUID") val keyBoardPassCodeNameUUID: String,
    @SerializedName("subUUID") val subUUID: String,
    @SerializedName("stpDeviceUUID") val stpDeviceUUID: String,
    @SerializedName("name") val name: String,
    @SerializedName("keyBoardPassCode") val keyBoardPassCode: String,
    @SerializedName("timestamp") val timestamp: Long = Timestamp(System.currentTimeMillis()).time,
)
