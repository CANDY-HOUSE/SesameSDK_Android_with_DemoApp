package co.candyhouse.sesame.ble.os3.base

import co.candyhouse.sesame.utils.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class SesameOS3BleCipher(val name: String, private var sessionKey: ByteArray, private var sault: ByteArray) {
    var encryptCounter: Long = 0.toLong()//int32 4byte-> 010000 小端
    var decryptCounter: Long = 0.toLong()
    internal fun encrypt(plaintext: ByteArray): ByteArray {
        val c: Cipher = Cipher.getInstance("AES/CCM/NoPadding", "BC")
        val nonce = encryptCounter.toBytes() + sault
        encryptCounter = encryptCounter.inc()
        val aesKeys = SecretKeySpec(sessionKey, "AES")
        val ivParameterSpec = GCMParameterSpec(32, nonce)
        c.init(Cipher.ENCRYPT_MODE, aesKeys, ivParameterSpec)
        c.updateAAD(byteArrayOf(0))
        return c.doFinal(plaintext)
    }

    internal fun decrypt(ciphertext: ByteArray): ByteArray {
        val nonce = decryptCounter.toBytes() + sault
//        L.d("hcia", "decryptCounter:" + decryptCounter)
        decryptCounter = decryptCounter.inc()
        val aesKeys = SecretKeySpec(sessionKey, "AES")
        val ivParameterSpec = GCMParameterSpec(32, nonce)
        val cipher: Cipher = Cipher.getInstance("AES/CCM/NoPadding", "BC")
        cipher.init(Cipher.DECRYPT_MODE, aesKeys, ivParameterSpec)
        cipher.updateAAD(byteArrayOf(0))
        return cipher.doFinal(ciphertext)
    }
}