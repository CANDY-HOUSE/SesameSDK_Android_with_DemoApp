package co.candyhouse.sesame.utils.aescmac

import java.util.*
import kotlin.experimental.or
import kotlin.experimental.xor

internal object AesUtil {
    const val BLOCK_SIZE = 16
    /**
     * Multiplies value by x in the finite field GF(2^128) represented using the primitive polynomial
     * x^128 + x^7 + x^2 + x + 1.
     *
     * @param value an arrays of 16 bytes representing an element of GF(2^128) using bigendian byte
     * order.
     */
    fun dbl(value: ByteArray): ByteArray {
        require(value.size == BLOCK_SIZE) { "value must be a block." }
        // Note that >> is an arithmetical shift, which copies the leftmost bit to fill the
// blanks created by shifting. For instance, x >> 7 will equal 0xFF if (x & 1), and 0x00
// otherwise. This is a bit hard to read, but the operation is branchless, which is valuable
// in this context.
// Shift left by one.
        val res = ByteArray(BLOCK_SIZE)
        for (i in 0 until BLOCK_SIZE) {
            res[i] = (0xFE and (value[i].toInt() shl 1)).toByte()
            if (i < BLOCK_SIZE - 1) {
                res[i] = res[i] or (0x01 and (value[i + 1].toInt()  shr 7)).toByte()
            }
        }
        // And handle the modulus if needed (0x87 is the binary representation of the polynomial,
// minus the x^128 part).
        res[BLOCK_SIZE - 1] = res[BLOCK_SIZE - 1] xor (0x87 and (value[0].toInt()  shr 7)).toByte()
        return res
    }

    /**
     * Pad by adding a 1 bit, then pad with 0 bits to the next block limit. This is the standard for
     * both CMAC and AES-SIV. - https://tools.ietf.org/html/rfc4493#section-2.4 -
     * https://tools.ietf.org/html/rfc5297#section-2.1
     *
     * @param x The array to pad (will be copied)
     * @return The padded array.
     */
    fun cmacPad(x: ByteArray): ByteArray {
        require(x.size < BLOCK_SIZE) { "x must be smaller than a block." }
        val result = Arrays.copyOf(x, 16)
        result[x.size] = 0x80.toByte()
        return result
    }
}
