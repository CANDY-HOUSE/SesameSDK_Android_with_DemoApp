package co.candyhouse.sesame.ble


internal enum class DeviceSegmentType(var value: Int) {
    plain(1), cipher(2), ;
    companion object {
        private val values = values()
        fun getByValue(value: Int) = values.first { it.value == value }
    }
}

internal class SesameBleReceiver {
    var buffer = byteArrayOf()
    internal fun feed(input: ByteArray): Pair<DeviceSegmentType, ByteArray>? {
        val segmentFlag = input[0]
        val isStartFlag = segmentFlag.toInt() and 1
        val parsingType = segmentFlag.toInt() shr 1
//        L.d("hcia", "isStartFlag:" + isStartFlag)
//        L.d("hcia", "parsingType:" + parsingType)
        if (isStartFlag > 0) {
            buffer = input.drop(1).toByteArray()
        } else {
            buffer += input.drop(1).toByteArray()
        }
        if (parsingType > 0) {
            val buf = buffer
            buffer = byteArrayOf()
            val type = DeviceSegmentType.getByValue(parsingType)
            return Pair(type, buf)
        } else {
            return null
        }
    }
}


internal class SesameBleTransmit(var type: DeviceSegmentType, var input: ByteArray) {
    var isStart = 1
    internal fun getChunk(): ByteArray? {
        if (isStart == -1) {

            return null
        } else if (input.size <= 19) {
            val segmentHeader = ((type.value shl 1) or isStart).toByte()
            isStart = -1
            return byteArrayOf(segmentHeader) + input
        } else {
            val payload = input.copyOf(19)
            val segmentHeader = isStart.toByte()
            input = input.drop(19).toByteArray()
            isStart = 0
            return byteArrayOf(segmentHeader) + payload
        }
    }
}

