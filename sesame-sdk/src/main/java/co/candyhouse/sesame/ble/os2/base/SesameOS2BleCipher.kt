package co.candyhouse.sesame.ble.os2.base

import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class SesameOS2BleCipher(private var sessionKey: ByteArray, var sessionToken: ByteArray) {
    var encryptCounter: Long = 0.toLong()
    var decryptCounter: Long = 0.toLong()

    internal fun encrypt(plaintext: ByteArray): ByteArray {
        val c: Cipher = Cipher.getInstance("AES/CCM/NoPadding", "BC")
        val nonce = encryptCounter.toEncryCounter() + sessionToken
        encryptCounter = encryptCounter.inc()
        val aesKeys = SecretKeySpec(sessionKey, "AES")
        val ivParameterSpec = GCMParameterSpec(32, nonce)
        c.init(Cipher.ENCRYPT_MODE, aesKeys, ivParameterSpec)
        c.updateAAD(byteArrayOf(0))
        return c.doFinal(plaintext)
    }

    internal fun decrypt(ciphertext: ByteArray): ByteArray {
        val nonce = decryptCounter.toDecryCounter() + sessionToken
//        L.d("hcia", "nonce:" + nonce.toHexString())
//        L.d("hcia", "sessionKey:" + sessionKey.toHexString())
        decryptCounter = decryptCounter.inc() and Long.MAX_VALUE
        val aesKeys = SecretKeySpec(sessionKey, "AES")
        val ivParameterSpec = GCMParameterSpec(32, nonce)
        val cipher: Cipher = Cipher.getInstance("AES/CCM/NoPadding", "BC")
        cipher.init(Cipher.DECRYPT_MODE, aesKeys, ivParameterSpec)
        cipher.updateAAD(byteArrayOf(0))
        return cipher.doFinal(ciphertext)
    }

}
internal fun Long.toEncryCounter(): ByteArray {
    var bytes = byteArrayOf()
    var testLong: Long = this
    testLong = testLong or 0x8000000000
    for (index in 0 until 5) {
        val b = testLong.toByte()
        bytes += b
        testLong = testLong.shr(Byte.SIZE_BITS)
    }
//    L.d("hcia", "bytes:" + bytes.toHexString())
    return bytes
}

internal fun Long.toDecryCounter(): ByteArray {
    var bytes = byteArrayOf()
    var testLong: Long = this
    testLong = testLong and 0x7fffffffff
    for (index in 0 until 5) {
        val b = testLong.toByte()
        bytes += b
        testLong = testLong.shr(Byte.SIZE_BITS)
    }
    return bytes
}
