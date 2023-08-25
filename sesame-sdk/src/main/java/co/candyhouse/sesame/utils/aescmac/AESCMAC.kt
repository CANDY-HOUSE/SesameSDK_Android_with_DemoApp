package co.candyhouse.sesame.utils.aescmac

import java.security.GeneralSecurityException
import java.security.InvalidAlgorithmParameterException
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec


internal  class AesCmac(key: ByteArray, tagSizeInBytes: Int) : Mac {
    val MIN_TAG_SIZE_IN_BYTES = 10
    private var keySpec: SecretKey? = null
    private var tagSizeInBytes = 0
    lateinit var subKey1: ByteArray
    lateinit var subKey2: ByteArray

    init {
        Validators.validateAesKeySize(key.size)
        if (tagSizeInBytes < MIN_TAG_SIZE_IN_BYTES) {
            throw InvalidAlgorithmParameterException(
                    "tag size too small, min is $MIN_TAG_SIZE_IN_BYTES bytes")
        }
        if (tagSizeInBytes > AesUtil.BLOCK_SIZE) {
            throw InvalidAlgorithmParameterException("tag size too large, max is " + AesUtil.BLOCK_SIZE.toString() + " bytes")
        }
        keySpec = SecretKeySpec(key, "AES")
        this.tagSizeInBytes = tagSizeInBytes
        generateSubKeys()
    }



    private fun instance(): Cipher? {
        return EngineFactory.CIPHER.getInstance("AES/ECB/NoPadding")
    }

    override fun computeMac(data: ByteArray?): ByteArray? {
        val aes = instance()
        aes!!.init(Cipher.ENCRYPT_MODE, keySpec)

        val n = Math.max(1, Math.ceil(data!!.size.toDouble() / AesUtil.BLOCK_SIZE).toInt())

        // Step 3
        // Step 3
        val flag = n * AesUtil.BLOCK_SIZE == data.size

        // Step 4
        // Step 4
        val mLast: ByteArray
        mLast = if (flag) {
            Bytes.xor(data, (n - 1) * AesUtil.BLOCK_SIZE, subKey1, 0, AesUtil.BLOCK_SIZE)
        } else {
            Bytes.xor(
                    AesUtil.cmacPad(Arrays.copyOfRange(data, (n - 1) * AesUtil.BLOCK_SIZE, data.size)),
                    subKey2)
        }

        // Step 5
        // Step 5
        var x: ByteArray? = ByteArray(AesUtil.BLOCK_SIZE)

        // Step 6
        // Step 6
        var y: ByteArray?
        for (i in 0 until n - 1) {
            y = Bytes.xor(x!!, 0, data, i * AesUtil.BLOCK_SIZE, AesUtil.BLOCK_SIZE)
            x = aes.doFinal(y)
        }
        y = Bytes.xor(mLast, x!!)

        // Step 7
        // Step 7
        val tag = ByteArray(tagSizeInBytes)
        System.arraycopy(aes.doFinal(y), 0, tag, 0, tagSizeInBytes)
        return tag
    }

    override fun verifyMac(mac: ByteArray?, data: ByteArray?) {
        if (!Bytes.equal(mac, computeMac(data))) {
            throw GeneralSecurityException("invalid MAC")
        }
    }

    @Throws(GeneralSecurityException::class)
    private fun generateSubKeys() {
        val aes = instance()
        aes!!.init(Cipher.ENCRYPT_MODE, keySpec)
        val zeroes = ByteArray(AesUtil.BLOCK_SIZE)
        val l = aes.doFinal(zeroes)
        subKey1 = AesUtil.dbl(l)
        subKey2 = AesUtil.dbl(subKey1)
    }
}