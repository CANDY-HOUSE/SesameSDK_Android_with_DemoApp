package candyhouse.sesameos.ir.base.irHttpClientBean

import android.health.connect.datatypes.units.Length
import com.google.gson.annotations.SerializedName

data class IrMatchCodeRequest(
    @SerializedName("irWave") val irWave: String,
    @SerializedName("irWaveLength") val irWaveLength: Int,
    @SerializedName("type") val type: Int,
    @SerializedName("brandName") val brandName: String,
) {

}