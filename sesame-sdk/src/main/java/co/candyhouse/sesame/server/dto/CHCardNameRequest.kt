package co.candyhouse.sesame.server.dto

import com.google.gson.annotations.SerializedName
import java.sql.Timestamp

// json data class
data class CHCardNameRequest(
    @SerializedName("cardType") val cardType: Byte,
    @SerializedName("cardNameUUID") val cardNameUUID: String,
    @SerializedName("subUUID") val subUUID: String,
    @SerializedName("stpDeviceUUID") val stpDeviceUUID: String,
    @SerializedName("name") val name: String,
    @SerializedName("cardID") val cardID: String,
    @SerializedName("timestamp") val timestamp: Long = Timestamp(System.currentTimeMillis()).time,
    @SerializedName("op") val op: String
)
