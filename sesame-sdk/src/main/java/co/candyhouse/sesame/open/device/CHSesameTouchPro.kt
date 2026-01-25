package co.candyhouse.sesame.open.device

import co.candyhouse.sesame.utils.bytesToShort
import com.google.gson.Gson

class CHSesameTouchProMechStatus(override val data: ByteArray) : CHSesameProtocolMechStatus {
    private val battery = bytesToShort(data[0], data[1])
    override fun getBatteryVoltage(): Float {// 確認設備電壓，getBatteryPrecentage 有必要再複寫。
        return battery * 2f / 1000f
    }
}

/** Open sensor */
class CHSesameOpenSensorMechStatus(openSensorData: OpenSensorData) : CHSesameProtocolMechStatus {
    override val data: ByteArray = openSensorData.toByteArray()
    private val battery: Short? = openSensorData.Battery
    private val lightLoadBatteryVoltage: Short? = openSensorData.lightLoadBatteryVoltage_mV
    private val heavyLoadBatteryVoltage: Short? = openSensorData.heavyLoadBatteryVoltage_mV

    override fun getBatteryVoltage(): Float {
        battery?.let {
            return it * 2f / 1000f
        }
        lightLoadBatteryVoltage?.let {
            // return (it + heavyLoadBatteryVoltage!!) / 1000f
            return it.toFloat() * 2f / 1000f
        }
        return 6.00f
    }

    // OpenSensor 用的电池是 CR1632, 与 Touch Pro 用的电池 CR123A 不同。
    override fun getBatteryPrecentage(): Int {
        val voltage = getBatteryVoltage()
        /*  在刷卡機的曲線上，實測低於8%的電量開始，會出現偶爾丟失藍牙信號的問題。低於2%就會關機，按reset物理按鍵，無響應。
        *   【修正 Open Sensor 1 電池電量顯示不准的問題】
        *   用 PPK 供電，實測 OpenSensor1 讀到的電壓值，比 PPK 的設置值，低 70mV 左右。
        *   Anyway..., CR1632 與 CR123A 同為 錳酸鋰LiMnO2 化學成分相同。但用戶抱怨新CR1632電池往往從 40% 開始,
        *   故哲明研判是內電阻等物理性質不同, 故基於CR123A的電池曲線, 全部統一減去0.3V, 5.85V-0.3V也就是CR123A約40%的電壓。
        *   放水讓用戶開心,電力曲線整體下降0.3V, 用更低的電壓對應更高的電量百分比。
        */
        val blocks: List<Float> = listOf(5.85f, 5.82f, 5.79f, 5.76f, 5.73f, 5.7f, 5.65f, 5.6f, 5.55f, 5.5f, 5.4f, 5.2f, 5.1f, 5.0f, 4.8f, 4.6f).map{ it - 0.3f }
        val mapping: List<Float> = listOf(100.0f, 95.0f, 90.0f, 85.0f, 80.0f, 70.0f, 60.0f, 50.0f, 40.0f, 32.0f, 21.0f, 13.0f, 10.0f, 7.0f, 3.0f, 0.0f)
        if (voltage >= blocks[0]) {
            return mapping[0].toInt()
        }
        if (voltage <= blocks[blocks.size - 1]) {
            return (mapping[mapping.size - 1]).toInt()
        }
        for (i in 0 until blocks.size - 1) {
            val upper: Float = blocks[i]
            val lower: Float = blocks[i + 1]
            if (voltage <= upper && voltage > lower) {
                val value: Float = (voltage - lower) / (upper - lower)
                val max = mapping[i]
                val min = mapping[i + 1]
                return ((max - min) * value + min).toInt()
            }
        }
        return 0
    }
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