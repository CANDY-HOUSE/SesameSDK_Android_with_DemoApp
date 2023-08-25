package co.candyhouse.sesame.utils.aescmac

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.util.*
import kotlin.experimental.xor

object Bytes {
    /**
     * Best effort fix-timing array comparison.
     *
     * @return true if two arrays are equal.
     */
    fun equal(x: ByteArray?, y: ByteArray?): Boolean {
        if (x == null || y == null) {
            return false
        }
        if (x.size != y.size) {
            return false
        }
        var res = 0
        for (i in x.indices) {
            res = res or x[i].toInt() xor y[i].toInt()
        }
        return res == 0
    }

    /**
     * Returns the concatenation of the input arrays in a single array. For example, `concat(new
     * byte[] {a, b}, new byte[] {}, new byte[] {c}` returns the array `{a, b, c}`.
     *
     * @return a single array containing all the values from the source arrays, in order
     */
//    @Throws(GeneralSecurityException::class)
    fun concat(vararg chunks: ByteArray): ByteArray {
        var length = 0
        for (chunk in chunks) {
            if (length > Int.MAX_VALUE - chunk.size) {
                throw GeneralSecurityException("exceeded size limit")
            }
            length += chunk.size
        }
        val res = ByteArray(length)
        var pos = 0
        for (chunk in chunks) {
            System.arraycopy(chunk, 0, res, pos, chunk.size)
            pos += chunk.size
        }
        return res
    }

    /**
     * Computes the xor of two byte arrays, specifying offsets and the length to xor.
     *
     * @return a new byte[] of length len.
     */
    fun xor(
            x: ByteArray, offsetX: Int, y: ByteArray, offsetY: Int, len: Int): ByteArray {
        require(!(len < 0 || x.size - len < offsetX || y.size - len < offsetY)) { "That combination of buffers, offsets and length to xor result in out-of-bond accesses." }
        val res = ByteArray(len)
        for (i in 0 until len) {
            res[i] = (x[i + offsetX] xor y[i + offsetY]) as Byte
        }
        return res
    }

    /**
     * Computes the xor of two byte buffers, specifying the length to xor, and
     * stores the result to `output`.
     *
     * @return a new byte[] of length len.
     */
    fun xor(output: ByteBuffer, x: ByteBuffer, y: ByteBuffer, len: Int) {
        require(!(len < 0 || x.remaining() < len || y.remaining() < len || output.remaining() < len)) { "That combination of buffers, offsets and length to xor result in out-of-bond accesses." }
        for (i in 0 until len) {
            output.put((x.get() xor y.get()) as Byte)
        }
    }

    /**
     * Computes the xor of two byte arrays of equal size.
     *
     * @return a new byte[] of length x.length.
     */
    fun xor(x: ByteArray, y: ByteArray): ByteArray {
        require(x.size == y.size) { "The lengths of x and y should match." }
        return xor(x, 0, y, 0, x.size)
    }

    /**
     * xors b to the end of a.
     *
     * @return a new byte[] of length x.length.
     */
    fun xorEnd(a: ByteArray, b: ByteArray): ByteArray {
        require(a.size >= b.size) { "xorEnd requires a.length >= b.length" }
        val paddingLength = a.size - b.size
        val res = Arrays.copyOf(a, a.size)
        for (i in b.indices) {
            res[paddingLength + i] = res[paddingLength + i] xor b[i]
        }
        return res
    }

    /**
     * Transforms a passed value to a LSB first byte array with the size of the specified capacity
     *
     * @param capacity size of the resulting byte array
     * @param value that should be represented as a byte array
     */
    fun intToByteArray(capacity: Int, value: Int): ByteArray {
        val result = ByteArray(capacity)
        for (i in 0 until capacity) {
            result[i] = (value shr 8 * i and 0xFF).toByte()
        }
        return result
    }
    /**
     * Transforms a passed LSB first byte array to an int
     *
     * @param bytes that should be transformed to a byte array
     * @param length amount of the passed `bytes` that should be transformed
     */
    /**
     * Transforms a passed LSB first byte array to an int
     *
     * @param bytes that should be transformed to a byte array
     */
    @JvmOverloads
    fun byteArrayToInt(bytes: ByteArray, length: Int = bytes.size): Int {
        return byteArrayToInt(bytes, 0, length)
    }

    /**
     * Transforms a passed LSB first byte array to an int
     *
     * @param bytes that should be transformed to a byte array
     * @param offset start index to start the transformation
     * @param length amount of the passed `bytes` that should be transformed
     */
    fun byteArrayToInt(bytes: ByteArray, offset: Int, length: Int): Int {
        var value = 0
        for (i in 0 until length) {
            value += bytes[i + offset].toInt() and 0xFF shl i * 8
        }
        return value
    }
}
