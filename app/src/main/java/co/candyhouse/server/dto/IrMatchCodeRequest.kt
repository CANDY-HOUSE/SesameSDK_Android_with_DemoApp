package co.candyhouse.server.dto

import com.google.gson.annotations.SerializedName

data class IrMatchCodeRequest(
    @SerializedName("irWave") val irWave: String,
    @SerializedName("irWaveLength") val irWaveLength: Int,
    @SerializedName("type") val type: Int,
    @SerializedName("brandName") val brandName: String,
) {

}