package co.candyhouse.sesame.utils.aescmac

import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.util.*

object SubtleUtil {
    /**r
     * Returns the Ecdsa algorithm name corresponding to a hash type.
     *
     * @param hash the hash type
     * @return the JCE's Ecdsa algorithm name for the hash.
     * @throw GeneralSecurityExceptio if `hash` is not supported or is not safe for digital
     * signature.
     */
    @Throws(GeneralSecurityException::class)
    fun toEcdsaAlgo(hash: Enums.HashType): String {
        Validators.validateSignatureHash(hash)
        return hash.toString() + "withECDSA"
    }

    /**
     * Returns the RSA SSA (Signature with Appendix) PKCS1 algorithm name corresponding to a hash
     * type.
     *
     * @param hash the hash type.
     * @return the JCE's RSA SSA PKCS1 algorithm name for the hash.
     * @throw GeneralSecurityException if `hash` is not supported or is not safe for digital
     * signature.
     */
    @Throws(GeneralSecurityException::class)
    fun toRsaSsaPkcs1Algo(hash: Enums.HashType): String {
        Validators.validateSignatureHash(hash)
        return hash.toString() + "withRSA"
    }

    /**
     * Returns the digest algorithm name corresponding to a hash type.
     *
     * @param hash the hash type.
     * @return theh JCE's hash algorithm name.
     * @throw GeneralSecurityException if `hash` is not supported.
     */
    @Throws(GeneralSecurityException::class)
    fun toDigestAlgo(hash: Enums.HashType): String {
        return when (hash) {
            Enums.HashType.SHA1 -> "SHA-1"
            Enums.HashType.SHA256 -> "SHA-256"
            Enums.HashType.SHA384 -> "SHA-384"
            Enums.HashType.SHA512 -> "SHA-512"
        }
        throw GeneralSecurityException("Unsupported hash $hash")
    }// If Application isn't loaded, it might as well not be Android./*initialize=*/

    /**
     * Best-effort checks that this is Android.
     *
     * @return true if running on Android.
     */
    val isAndroid: Boolean
        get() = try {
            Class.forName("android.app.Application",  /*initialize=*/false, null)
            true
        } catch (e: Exception) { // If Application isn't loaded, it might as well not be Android.
            false
        }

    /**
     * Converts an byte array to a nonnegative integer
     * (https://tools.ietf.org/html/rfc8017#section-4.1).
     *
     * @param bs the byte array to be converted to integer.
     * @return the corresponding integer.
     */
    fun bytes2Integer(bs: ByteArray?): BigInteger {
        return BigInteger(1, bs)
    }

    /**
     * Converts a nonnegative integer to a byte array of a specified length
     * (https://tools.ietf.org/html/rfc8017#section-4.2).
     *
     * @param num nonnegative integer to be converted.
     * @param intendedLength intended length of the resulting integer.
     * @return the corresponding byte array of length `intendedLength`.
     */
    @Throws(GeneralSecurityException::class)
    fun integer2Bytes(num: BigInteger, intendedLength: Int): ByteArray {
        val b = num.toByteArray()
        if (b.size == intendedLength) {
            return b
        }
        if (b.size > intendedLength + 1 /* potential leading zero */) {
            throw GeneralSecurityException("integer too large")
        }
        if (b.size == intendedLength + 1) {
            return if (b[0].toInt() == 0 /* leading zero */) {
                Arrays.copyOfRange(b, 1, b.size)
            } else {
                throw GeneralSecurityException("integer too large")
            }
        }
        // Left zero pad b.
        val res = ByteArray(intendedLength)
        System.arraycopy(b, 0, res, intendedLength - b.size, b.size)
        return res
    }

    /** Computes MGF1 as defined at https://tools.ietf.org/html/rfc8017#appendix-B.2.1.  */
    @Throws(GeneralSecurityException::class)
    fun mgf1(mgfSeed: ByteArray?, maskLen: Int, mgfHash: Enums.HashType): ByteArray {
        val digest = EngineFactory.MESSAGE_DIGEST.getInstance(toDigestAlgo(mgfHash))
        val hLen = digest.digestLength
        // Step 1. Check maskLen.
// As max integer is only 2^31 - 1 which is smaller than the limit 2^32, this step is skipped.
// Step 2, 3. Compute t.
        val t = ByteArray(maskLen)
        var tPos = 0
        for (counter in 0..(maskLen - 1) / hLen) {
            digest.reset()
            digest.update(mgfSeed)
            digest.update(integer2Bytes(BigInteger.valueOf(counter.toLong()), 4))
            val c = digest.digest()
            System.arraycopy(c, 0, t, tPos, Math.min(c.size, t.size - tPos))
            tPos += c.size
        }
        return t
    }

    /**
     * Inserts {@param value} as unsigned into into {@param buffer}.
     *
     *
     * @throws GeneralSecurityException if not 0 <= value < 2^32.
     */
    @Throws(GeneralSecurityException::class)
    fun putAsUnsigedInt(buffer: ByteBuffer, value: Long) {
        if (!(0 <= value && value < 0x100000000L)) {
            throw GeneralSecurityException("Index out of range")
        }
        buffer.putInt(value.toInt())
    }
}
