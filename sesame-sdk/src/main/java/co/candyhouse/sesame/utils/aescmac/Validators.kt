package co.candyhouse.sesame.utils.aescmac

import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.InvalidAlgorithmParameterException
import java.util.regex.Pattern

class Enums {
    /** Hash type.  */
    enum class HashType {
        SHA1,  // Using SHA1 for digital signature is deprecated but HMAC-SHA1 is fine.
        SHA256, SHA384, SHA512
    }
}
object Validators {
    private const val TYPE_URL_PREFIX = "type.googleapis.com/"
    /**
     * To reach 128-bit security strength, RSA's modulus must be at least 3072-bit while 2048-bit RSA
     * key only has 112-bit security. Nevertheless, a 2048-bit RSA key is considered safe by NIST
     * until 2030 (see https://www.keylength.com/en/4/).
     */
    private const val MIN_RSA_MODULUS_SIZE = 2048

    /** @throws GeneralSecurityException if `typeUrl` is in invalid format.
     */
    @Throws(GeneralSecurityException::class)
    fun validateTypeUrl(typeUrl: String) {
        if (!typeUrl.startsWith(TYPE_URL_PREFIX)) {
            throw GeneralSecurityException(String.format(
                    "Error: type URL %s is invalid; it must start with %s.\n", typeUrl, TYPE_URL_PREFIX))
        }
        if (typeUrl.length == TYPE_URL_PREFIX.length) {
            throw GeneralSecurityException(String.format("Error: type URL %s is invalid; it has no message name.\n", typeUrl))
        }
    }

    /** @throws InvalidAlgorithmParameterException if `sizeInBytes` is not supported.
     */
    @Throws(InvalidAlgorithmParameterException::class)
    fun validateAesKeySize(sizeInBytes: Int) {
        if (sizeInBytes != 16 && sizeInBytes != 32) {
            throw InvalidAlgorithmParameterException(String.format(
                    "invalid key size %d; only 128-bit and 256-bit AES keys are supported",
                    sizeInBytes * 8))
        }
    }

    /**
     * @throws GeneralSecurityException if `candidate` is negative or larger than `maxExpected`.
     */
    @Throws(GeneralSecurityException::class)
    fun validateVersion(candidate: Int, maxExpected: Int) {
        if (candidate < 0 || candidate > maxExpected) {
            throw GeneralSecurityException(String.format(
                    "key has version %d; only keys with version in range [0..%d] are supported",
                    candidate, maxExpected))
        }
    }

    /**
     * Validates whether `hash` is safe to use for digital signature.
     *
     * @throws GeneralSecurityException if `hash` is invalid or is not safe to use for digital
     * signature.
     */
    @Throws(GeneralSecurityException::class)
    fun validateSignatureHash(hash: Enums.HashType) {
        when (hash) {
            Enums.HashType.SHA256, Enums.HashType.SHA384, Enums.HashType.SHA512 -> return
            else -> {
            }
        }
        throw GeneralSecurityException("Unsupported hash: " + hash.name)
    }

    /**
     * Validates whether `modulusSize` is at least 2048-bit.
     *
     *
     * To reach 128-bit security strength, RSA's modulus must be at least 3072-bit while 2048-bit
     * RSA key only has 112-bit security. Nevertheless, a 2048-bit RSA key is considered safe by NIST
     * until 2030 (see https://www.keylength.com/en/4/).
     *
     * @throws GeneralSecurityException if `modulusSize` is less than 2048-bit.
     */
    @Throws(GeneralSecurityException::class)
    fun validateRsaModulusSize(modulusSize: Int) {
        if (modulusSize < MIN_RSA_MODULUS_SIZE) {
            throw GeneralSecurityException(String.format(
                    "Modulus size is %d; only modulus size >= 2048-bit is supported", modulusSize))
        }
    }

    /*
   * @throws IOException if {@code f} exists.
   */
    @Throws(IOException::class)
    fun validateNotExists(f: File) {
        if (f.exists()) {
            throw IOException(String.format("%s exists, please choose another file\n", f.toString()))
        }
    }

    /** @throws IOException if `f` does not exists.
     */
    @Throws(IOException::class)
    fun validateExists(f: File) {
        if (!f.exists()) {
            throw IOException(String.format("Error: %s doesn't exist, please choose another file\n", f.toString()))
        }
    }

    /**
     * Validates that `kmsKeyUri` starts with `expectedPrefix`, and removes the prefix.
     *
     * @throws IllegalArgumentException
     */
    @Throws(IllegalArgumentException::class)
    fun validateKmsKeyUriAndRemovePrefix(expectedPrefix: String, kmsKeyUri: String): String {
        require(kmsKeyUri.toLowerCase().startsWith(expectedPrefix)) { String.format("key URI must start with %s", expectedPrefix) }
        return kmsKeyUri.substring(expectedPrefix.length)
    }

    // See https://tools.ietf.org/html/rfc3986#section-2.3.
    private const val URI_UNRESERVED_CHARS = "([0-9a-zA-Z\\-\\.\\_~])+"
    private val GCP_KMS_CRYPTO_KEY_PATTERN = Pattern.compile(String.format(
            "^projects/%s/locations/%s/keyRings/%s/cryptoKeys/%s$",
            URI_UNRESERVED_CHARS,
            URI_UNRESERVED_CHARS,
            URI_UNRESERVED_CHARS,
            URI_UNRESERVED_CHARS),
            Pattern.CASE_INSENSITIVE)
    private val GCP_KMS_CRYPTO_KEY_VERSION_PATTERN = Pattern.compile(String.format(
            "^projects/%s/locations/%s/keyRings/%s/cryptoKeys/%s/cryptoKeyVersions/%s$",
            URI_UNRESERVED_CHARS,
            URI_UNRESERVED_CHARS,
            URI_UNRESERVED_CHARS,
            URI_UNRESERVED_CHARS,
            URI_UNRESERVED_CHARS),
            Pattern.CASE_INSENSITIVE)

    /**
     * @throws GeneralSecurityException if `kmsKeyUri` is not a valid URI of a CryptoKey in
     * Google Cloud KMS.
     */
    @Throws(GeneralSecurityException::class)
    fun validateCryptoKeyUri(kmsKeyUri: String?) {
        if (!GCP_KMS_CRYPTO_KEY_PATTERN.matcher(kmsKeyUri).matches()) {
            if (GCP_KMS_CRYPTO_KEY_VERSION_PATTERN.matcher(kmsKeyUri).matches()) {
                throw GeneralSecurityException("Invalid Google Cloud KMS Key URI. "
                        + "The URI must point to a CryptoKey, not a CryptoKeyVersion")
            }
            throw GeneralSecurityException(
                    "Invalid Google Cloud KMS Key URI. "
                            + "The URI must point to a CryptoKey in the format "
                            + "projects/*/locations/*/keyRings/*/cryptoKeys/*. "
                            + "See https://cloud.google.com/kms/docs/reference/rest/v1"
                            + "/projects.locations.keyRings.cryptoKeys#CryptoKey")
        }
    }
}
