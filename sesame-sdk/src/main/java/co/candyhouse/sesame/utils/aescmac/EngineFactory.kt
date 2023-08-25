package co.candyhouse.sesame.utils.aescmac

import java.security.*
import java.util.*
import java.util.logging.Logger
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac

class EngineFactory<T_WRAPPER : EngineWrapper<T_ENGINE>?, T_ENGINE> {
    companion object {
        private val logger = Logger.getLogger(EngineFactory::class.java.name)
        private var defaultPolicy: List<Provider?>? = null
        private const val DEFAULT_LET_FALLBACK = true
        val CIPHER = EngineFactory(EngineWrapper.TCipher())
        fun getCustomCipherProvider(
                letFallbackToDefault: Boolean, vararg providerNames: String?): EngineFactory<EngineWrapper.TCipher, Cipher> {
            return EngineFactory(
                    EngineWrapper.TCipher(), toProviderList(*providerNames), letFallbackToDefault)
        }

        val MAC = EngineFactory(EngineWrapper.TMac())
        fun getCustomMacProvider(
                letFallbackToDefault: Boolean, vararg providerNames: String?): EngineFactory<EngineWrapper.TMac, Mac> {
            return EngineFactory(
                    EngineWrapper.TMac(), toProviderList(*providerNames), letFallbackToDefault)
        }

        val SIGNATURE = EngineFactory(EngineWrapper.TSignature())
        fun getCustomSignatureProvider(
                letFallbackToDefault: Boolean, vararg providerNames: String?): EngineFactory<EngineWrapper.TSignature, Signature> {
            return EngineFactory(
                    EngineWrapper.TSignature(), toProviderList(*providerNames), letFallbackToDefault)
        }

        val MESSAGE_DIGEST = EngineFactory(EngineWrapper.TMessageDigest())
        fun getCustomMessageDigestProvider(letFallbackToDefault: Boolean, vararg providerNames: String?): EngineFactory<EngineWrapper.TMessageDigest, MessageDigest> {
            return EngineFactory(
                    EngineWrapper.TMessageDigest(), toProviderList(*providerNames), letFallbackToDefault)
        }

        val KEY_AGREEMENT = EngineFactory(EngineWrapper.TKeyAgreement())
        fun getCustomKeyAgreementProvider(letFallbackToDefault: Boolean, vararg providerNames: String?): EngineFactory<EngineWrapper.TKeyAgreement, KeyAgreement> {
            return EngineFactory(
                    EngineWrapper.TKeyAgreement(), toProviderList(*providerNames), letFallbackToDefault)
        }

        val KEY_PAIR_GENERATOR = EngineFactory(EngineWrapper.TKeyPairGenerator())
        fun getCustomKeyPairGeneratorProvider(letFallbackToDefault: Boolean, vararg providerNames: String?): EngineFactory<EngineWrapper.TKeyPairGenerator, KeyPairGenerator> {
            return EngineFactory(
                    EngineWrapper.TKeyPairGenerator(), toProviderList(*providerNames), letFallbackToDefault)
        }

        val KEY_FACTORY = EngineFactory(EngineWrapper.TKeyFactory())
        fun getCustomKeyFactoryProvider(letFallbackToDefault: Boolean, vararg providerNames: String?): EngineFactory<EngineWrapper.TKeyFactory, KeyFactory> {
            return EngineFactory(
                    EngineWrapper.TKeyFactory(), toProviderList(*providerNames), letFallbackToDefault)
        }

        /** Helper function to get a list of Providers from names.  */
        fun toProviderList(vararg providerNames: String?): List<Provider?> {
            val providers: MutableList<Provider?> = ArrayList()
            for (s in providerNames) {
                val p = Security.getProvider(s)
                if (p != null) {
                    providers.add(p)
                } else {
                    logger.info(String.format("Provider %s not available", s))
                }
            }
            return providers
        }

        // Warning: keep this above the initialization of static providers below. or you'll get null
// pointer errors (due to this policy not being initialized).
        init {
            defaultPolicy = if (SubtleUtil.isAndroid) { 
                toProviderList(
                        "GmsCore_OpenSSL" /* Conscrypt in GmsCore, updatable thus preferrable */,
                        "AndroidOpenSSL" /* Conscrypt in AOSP, not updatable but still better than BC */)
            } else {
                ArrayList()
            }
        }
    }

    constructor(instanceBuilder: T_WRAPPER) {
        this.instanceBuilder = instanceBuilder
        policy = defaultPolicy
        letFallback = DEFAULT_LET_FALLBACK
    }

    constructor(instanceBuilder: T_WRAPPER, policy: List<Provider?>?) {
        this.instanceBuilder = instanceBuilder
        this.policy = policy
        letFallback = DEFAULT_LET_FALLBACK
    }

    constructor(instanceBuilder: T_WRAPPER, policy: List<Provider?>?, letFallback: Boolean) {
        this.instanceBuilder = instanceBuilder
        this.policy = policy
        this.letFallback = letFallback
    }

    @Throws(GeneralSecurityException::class)
    fun getInstance(algorithm: String?): T_ENGINE {
        var cause: Exception? = null
        policy = defaultPolicy

        for (provider in policy!!) {
            try {
                return instanceBuilder!!.getInstance(algorithm, provider)
            } catch (e: Exception) {
                if (cause == null) {
                    cause = e
                }
            }
        }
        if (letFallback) {
            return instanceBuilder!!.getInstance(algorithm, null)
        }
        throw GeneralSecurityException("No good Provider found.", cause)
    }

    private var instanceBuilder: T_WRAPPER
    private var policy: List<Provider?>?
    private var letFallback: Boolean
}
