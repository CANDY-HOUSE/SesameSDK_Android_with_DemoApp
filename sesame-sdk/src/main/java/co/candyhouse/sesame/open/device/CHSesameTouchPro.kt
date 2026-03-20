package co.candyhouse.sesame.open.device

import com.google.gson.Gson

class CHSesameTouchProMechStatus(override val data: ByteArray) : CHSesameProtocolMechStatus {}

/** Open sensor */
class CHSesameOpenSensorMechStatus(openSensorData: OpenSensorData) : CHSesameProtocolMechStatus {
    override val data: ByteArray = openSensorData.toByteArray()
    private val battery: Short? = openSensorData.Battery
    private val lightLoadBatteryVoltage: Short? = openSensorData.lightLoadBatteryVoltage_mV
    private val heavyLoadBatteryVoltage: Short? = openSensorData.heavyLoadBatteryVoltage_mV
}

data class OpenSensorData(
    val Status: String,
    val TimeStamp: Long,
    val Battery: Short?,
    val lightLoadBatteryVoltage_mV: Short?,
    val heavyLoadBatteryVoltage_mV: Short?,
) {
    fun toByteArray(): ByteArray {
        val gson = Gson()
        return gson.toJson(this).toByteArray(Charsets.UTF_8)
    }

    companion object {
        fun fromByteArray(byteArray: ByteArray): OpenSensorData {
            val gson = Gson()
            val jsonString = String(byteArray, Charsets.UTF_8)
            return gson.fromJson(jsonString, OpenSensorData::class.java)
        }
    }
}