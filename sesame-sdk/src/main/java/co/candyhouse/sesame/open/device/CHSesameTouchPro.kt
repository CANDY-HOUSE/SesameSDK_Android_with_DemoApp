package co.candyhouse.sesame.open.device

import co.candyhouse.sesame.ble.os3.CHRemoteNanoTriggerSettings
import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.server.dto.CHCardNameRequest
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.utils.L
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
    private val battery: Int = openSensorData.Battery

    override fun getBatteryVoltage(): Float {
        return battery * 2f / 1000f
    }

    // OpenSensor 用的电池是 CR1632, 与 Touch Pro 用的电池 CR123A 不同。
    override fun getBatteryPrecentage(): Int {
        val voltage = getBatteryVoltage()
        L.d("voltage", voltage.toString())
        /*
        *    修正电池电量显示不准的问题。
        *    在刷卡机的曲线上， 实测低于8%的电量开始， 会出现偶尔丢失蓝牙信号的问题。
        *    低于2%就会关机，按reset物理按键，无响应。
        *    用 PPK 供电， 实测， OpenSensor 读到的电压值， 比 PPK 的设置值， 低 70mV 左右。
        *    根据 CR1632 电池的规格书上的放电曲线图修正后， CR1632 电量显示表格如下：
        * */
        val blocks: List<Float> = listOf(5.820f, 5.810f, 5.755f, 5.735f, 5.665f, 5.620f, 5.585f, 5.556f, 5.550f, 5.530f, 5.450f, 5.400f, 5.320f, 5.280f, 5.225f, 5.150f)
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
    val Battery: Int
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