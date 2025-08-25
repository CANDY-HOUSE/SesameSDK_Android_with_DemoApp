package candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase

import co.candyhouse.sesame.utils.L

/**
 * 空调遥控器 指令处理
 */
class HXDCommandProcessor {
    private val tag = HXDCommandProcessor::class.java.simpleName
    private var power: Int = 0x00
    private var temperature: Int = 25
    private var fanSpeed: Int = 0x01
    private var windDirection: Int = 0x02
    private var autoWindDirection: Int = 0x01
    private var mode: Int = 0x02
    private var key: Int = 0x01
    private var code = 0x00
    private val defaultTable: MutableList<UInt> = mutableListOf(0u, 0u, 0u)
    private val AirPrefixCode: ByteArray = byteArrayOf(0x30, 0x01)
    private val commonPrefixCode: ByteArray = byteArrayOf(0x30, 0x00)
    @OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
     fun buildAirCommand(): UByteArray {
        val buf: UByteArray = buildKeyData(AirPrefixCode, code,defaultTable)
        buf[4] = temperature.toUByte()
        buf[5] = fanSpeed.toUByte()
        buf[6] = windDirection.toUByte()
        buf[7] = autoWindDirection.toUByte()
        buf[8] = power.toUByte()
        buf[9] = key.toByte().toUByte()
        buf[10] = mode.toUByte()
        buf[buf.size - 2] = 0xFF.toByte().toUByte()
        val checkSum = buf.dropLast(1).sumOf { it.toInt() }
        buf[buf.size - 1] = checkSum.toByte().toUByte()
        L.Companion.d(tag, "buildAirCommand: ${buf.toUByteArray().toHexString()}")
        return buf
    }


    @OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
    fun buildNoneAirCommand(): UByteArray {
        val buf: UByteArray = buildKeyData(commonPrefixCode, code,defaultTable)
        buf[9] = key.toByte().toUByte()

        buf[buf.size - 2] = 0xFF.toByte().toUByte()
        val checkSum = buf.dropLast(1).sumOf { it.toInt() }
        buf[buf.size - 1] = checkSum.toByte().toUByte()
        L.Companion.d(tag, "buildCommonCommand: ${buf.toUByteArray().toHexString()}")
        return buf
    }

    @OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
    fun buildKeyData(prefixCodeArray: ByteArray, code: Int, arcTable:MutableList<UInt>): UByteArray {
        val buf = mutableListOf<UByte>()
        buf.addAll(prefixCodeArray.map { it.toUByte() })
        buf.addAll(decimalToTwoHexInts(code).map { it.toUByte() })
        buf.addAll(UByteArray(7) { 0u })
        val sb = StringBuilder()
        arcTable.forEach { sb.append(it.toUByte().toHexString()+"   ") }
        arcTable[0] = arcTable[0] + 1u
        buf.addAll(arcTable.map { it.toUByte() })
        buf.add(0xFF.toUByte())
        buf.add(0u)
        return buf.toUByteArray()
    }

    fun decimalToTwoHexInts(number: Int): ByteArray {
        val firstPart = number / (0xff + 1)
        val secondPart = number % (0xff + 1)
        return byteArrayOf(firstPart.toByte(), secondPart.toByte())
    }

    fun setPower(power:Int): HXDCommandProcessor {
        this.power = power
        return this
    }
    fun getPower(): Int {
        return this.power
    }
    fun setTemperature(temperature: Int): HXDCommandProcessor {
        this.temperature = temperature
        return this
    }
    fun getTemperature(): Int {
        return this.temperature
    }
    fun setModel(model:Int): HXDCommandProcessor {
        this.mode = model
        return this
    }
    fun getModel(): Int {
        return this.mode
    }
    fun setFanSpeed(windRate:Int): HXDCommandProcessor {
        this.fanSpeed = windRate
        return this
    }
    fun getFanSpeed(): Int {
        return this.fanSpeed
    }
    fun setWindDirection(windDirection:Int): HXDCommandProcessor {
        this.windDirection = windDirection
        return this
    }
    fun getWindDirection(): Int {
        return this.windDirection
    }
    fun setAutoWindDirection(autoWindDirection:Int): HXDCommandProcessor {
        this.autoWindDirection = autoWindDirection
        return this
    }
    fun getAutoDirection(): Int {
        return this.autoWindDirection
    }

    fun setKey(key:Int): HXDCommandProcessor {
        this.key = key
        return this
    }
    fun getKey(): Int {
        return this.key
    }

    fun setCode(code:Int): HXDCommandProcessor {
        this.code = code
        return this
    }
    fun getCode(): Int {
        return this.code
    }


    @OptIn(ExperimentalUnsignedTypes::class)
    fun parseAirData(state: String):Boolean {
        if (state.isEmpty()) {
            L.Companion.d("","Error: Empty input")
            return false
        }
        if (!state.matches("[0-9A-Fa-f]+".toRegex())) {
            L.Companion.d("","Error: Invalid hex characters in input")
            return false
        }
        val data = state.chunked(2).map { it.toInt(16).toUByte() }.toUByteArray()

        if (data.size <= 10){
            return false
        }
        try {
            temperature = data[4].toInt()
            fanSpeed = data[5].toInt()
            windDirection = data[6].toInt()
            autoWindDirection = data[7].toInt()
            power = data[8].toInt()
            mode = data[10].toInt()
            L.Companion.d("parseAirData", "temperature: $temperature, windRate: $fanSpeed, windDirection: $windDirection, automaticWindDirection: $autoWindDirection, power: $power, mode: $mode")
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

}