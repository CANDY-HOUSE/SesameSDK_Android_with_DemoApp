package co.candyhouse.sesame.utils.aescmac

import java.security.GeneralSecurityException

interface Mac {
    /**
     * Computes message authentication code (MAC) for `data`.
     *
     * @return MAC value
     */
    @Throws(GeneralSecurityException::class)
    fun computeMac(data: ByteArray?): ByteArray?

    /**
     * Verifies whether `mac` is a correct authentication code (MAC) for `data`.
     *
     * @throws GeneralSecurityException if `mac` is not a correct MAC for `data`
     */
    @Throws(GeneralSecurityException::class)
    fun verifyMac(mac: ByteArray?, data: ByteArray?)
}
