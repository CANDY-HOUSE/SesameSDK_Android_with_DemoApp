package co.candyhouse.sesame.utils

import android.util.Base64

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.experimental.and





internal fun String.base64decodeByteArray(): ByteArray {
    return Base64.decode(this, Base64.DEFAULT)
}

internal fun ByteArray.divideArray(chunksize: Int): Array<ByteArray> {
    val ret = Array(Math.ceil(this.size / chunksize.toDouble()).toInt()) { ByteArray(chunksize) }
    var start = 0
    var parts = 0
    for (i in ret.indices) {
        if (start + chunksize > this.size) {
            System.arraycopy(this, start, ret[i], 0, this.size - start)
        } else {
            System.arraycopy(this, start, ret[i], 0, chunksize)
        }
        start += chunksize
        parts++
    }
    return ret
}

internal fun String.base64decodeHex(): String {
    val data = Base64.decode(this, Base64.DEFAULT)
    return data.toHexString()
}

internal fun String.noHashtoUUID(): UUID? {
    val input = this
    val sdsd = input.length - 1
    val testid = input.substring(0..7) + "-" + input.substring(8..11) + "-" + input.substring(12..15) + "-" + input.substring(16..19) + "-" + input.substring(20..sdsd)
    return UUID.fromString(testid)
}

fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
fun ByteArray.HexLog() = joinToString("") { " %02X".format(it) } + " [" + this.size+ "bytes]"

internal fun ByteArray.base64Encode(): String {
    val ss = Base64.encode(this.clone(), Base64.DEFAULT)

    return String(ss, StandardCharsets.UTF_8).replace("\n", "")
}


internal fun String.hexStringToByteArray() =
    ByteArray(this.length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

internal var fixheader = "3059301306072a8648ce3d020106082a8648ce3d03010703420004"

internal fun String.cutEccHeader(): String {
    val cutPb: String = this.replace(fixheader, "")
    return cutPb
}


internal fun ByteArray.toBigLong(): Long {
    return java.lang.Long.parseLong(this.reversedArray().toHexString(), 16)
}


internal fun ByteArray.toCutedHistag(): ByteArray? {
    val tagcount = this[0]
//    L.d("hcia", "this:" + this.toHexString())
//    L.d("hcia", "tagcount.toInt():" + tagcount.toInt())
    if (tagcount.toInt() == 0) {
        return null
    } else {
        return this.sliceArray(1..tagcount.toInt())
    }
}

internal fun ByteArray.toInt(): Int {
    var result: Int = 0
    for (i in 0..1) {
        result = result shl 8
        result = result or (this[i] and 0xFF.toByte()).toInt()
    }
    return result
}


internal fun bytesToShort(byte1: Byte, byte2: Byte): Short {

    return (((byte2.toInt() and 0xFF) shl 8) or (byte1.toInt() and 0xFF)).toShort()
}


internal fun Short.toReverseBytes(): ByteArray {
    val buffer: ByteBuffer = ByteBuffer.allocate(2)
    buffer.putShort(this)
    return byteArrayOf(buffer[1], buffer[0])
}

internal fun Long.toBytes(isBigEndian: Boolean = false): ByteArray {
    var bytes = byteArrayOf()
    var testLong: Long = this
    for (index in 0 until Long.SIZE_BYTES) {
        val b = testLong.toByte()
        bytes += b
        testLong = testLong.shr(Byte.SIZE_BITS)
//            L.d("hcia", "index:" + index)
    }
//    L.d("hcia", "bytes:" + bytes.toHexString())
    return if (isBigEndian) {
        bytes.reversedArray()
    } else {
        bytes
    }
}


internal fun generateRandomData(count: Int): ByteArray {
    val b = ByteArray(count)
    Random().nextBytes(b)
    return b
}

internal fun Long.toUInt32ByteArray(): ByteArray {
//    ["🧕 initialize", 1605929466.48249, 1605929466, "fa89b85f"]
    val tmp = this / 1000
    val bytes = ByteArray(4)
    bytes[3] = (tmp and 0xFFFF).toByte()
    bytes[2] = ((tmp ushr 8) and 0xFFFF).toByte()
    bytes[1] = ((tmp ushr 16) and 0xFFFF).toByte()
    bytes[0] = ((tmp ushr 24) and 0xFFFF).toByte()
    return bytes.reversedArray()
}

internal fun Long.toUInt24ByteArray(): ByteArray {
//    ["🧕 initialize", 1605929466.48249, 1605929466, "fa89b85f"]
    val tmp = this / 1000
    val bytes = ByteArray(3)
//    bytes[3] = (tmp and 0xFFFF).toByte()
    bytes[2] = ((tmp ushr 8) and 0xFFFF).toByte()
    bytes[1] = ((tmp ushr 16) and 0xFFFF).toByte()
    bytes[0] = ((tmp ushr 24) and 0xFFFF).toByte()
    return bytes.reversedArray()
}

