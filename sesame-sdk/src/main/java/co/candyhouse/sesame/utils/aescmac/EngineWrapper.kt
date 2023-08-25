package co.candyhouse.sesame.utils.aescmac

import java.security.*
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac

interface EngineWrapper<T> {
    /** Cipher wrapper.  */
    class TCipher : EngineWrapper<Cipher> {
        @Throws(GeneralSecurityException::class)
        override fun getInstance(algorithm: String?, provider: Provider?): Cipher {
            return if (provider == null) {
                Cipher.getInstance(algorithm)
            } else {
                Cipher.getInstance(algorithm, provider)
            }
        }
    }

    /** Mac wrapper.  */
    class TMac : EngineWrapper<Mac> {
        @Throws(GeneralSecurityException::class)
        override fun getInstance(algorithm: String?, provider: Provider?): Mac {
            return if (provider == null) {
                Mac.getInstance(algorithm)
            } else {
                Mac.getInstance(algorithm, provider)
            }
        }
    }

    /** KeyPairGenerator wrapper.  */
    class TKeyPairGenerator : EngineWrapper<KeyPairGenerator> {
        @Throws(GeneralSecurityException::class)
        override fun getInstance(algorithm: String?, provider: Provider?): KeyPairGenerator {
            return if (provider == null) {
                KeyPairGenerator.getInstance(algorithm)
            } else {
                KeyPairGenerator.getInstance(algorithm, provider)
            }
        }
    }

    /** MessageDigest wrapper.  */
    class TMessageDigest : EngineWrapper<MessageDigest> {
        @Throws(GeneralSecurityException::class)
        override fun getInstance(algorithm: String?, provider: Provider?): MessageDigest {
            return if (provider == null) {
                MessageDigest.getInstance(algorithm!!)
            } else {
                MessageDigest.getInstance(algorithm!!, provider)
            }
        }
    }

    /** Signature wrapper.  */
    class TSignature : EngineWrapper<Signature> {
        @Throws(GeneralSecurityException::class)
        override fun getInstance(algorithm: String?, provider: Provider?): Signature {
            return if (provider == null) {
                Signature.getInstance(algorithm)
            } else {
                Signature.getInstance(algorithm, provider)
            }
        }
    }

    /** KeyFactory wrapper.  */
    class TKeyFactory : EngineWrapper<KeyFactory> {
        @Throws(GeneralSecurityException::class)
        override fun getInstance(algorithm: String?, provider: Provider?): KeyFactory {
            return if (provider == null) {
                KeyFactory.getInstance(algorithm)
            } else {
                KeyFactory.getInstance(algorithm, provider)
            }
        }
    }

    /** KeyAgreement wrapper.  */
    class TKeyAgreement : EngineWrapper<KeyAgreement> {
        @Throws(GeneralSecurityException::class)
        override fun getInstance(algorithm: String?, provider: Provider?): KeyAgreement {
            return if (provider == null) {
                KeyAgreement.getInstance(algorithm)
            } else {
                KeyAgreement.getInstance(algorithm, provider)
            }
        }
    }

    /** Should call T.getInstance(...).  */
    @Throws(GeneralSecurityException::class)
    fun getInstance(algorithm: String?, provider: Provider?): T
}
