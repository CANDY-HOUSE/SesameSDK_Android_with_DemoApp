package co.candyhouse.sesame.utils

import android.annotation.SuppressLint
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement


internal object EccKey {

    private var keyPairA: KeyPair? = null

    internal fun getRegisterAK(): String {
        return getDeviceECCKey().public.encoded.toHexString().cutEccHeader().hexStringToByteArray().base64Encode()
    }

    internal fun getPubK(): String {
        return getDeviceECCKey().public.encoded.toHexString().cutEccHeader()
    }

    internal fun ecdh(remotePublicKey: ByteArray): ByteArray {
        val remoteECPub: ECPublicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(fixheader.hexStringToByteArray() + remotePublicKey)) as ECPublicKey
        val kaA = KeyAgreement.getInstance("ECDH")
        kaA.init(getDeviceECCKey().private)
        kaA.doPhase(remoteECPub, true)
        return kaA.generateSecret()
    }

    @SuppressLint("CheckResult")
    internal fun getDeviceECCKey(): KeyPair {
        keyPairA?.let { return it }
        val keyGen = KeyPairGenerator.getInstance("EC")
        keyGen.initialize(ECGenParameterSpec("secp256r1")) //prime256v1 == secp256r1 == NIST P-256  secp256r1 是基于椭圆曲线 y² = x³ - 3x + b
        val newKeyPairA = keyGen.generateKeyPair()
        keyPairA = newKeyPairA
        return newKeyPairA
    }

}