package co.candyhouse.sesame.ble.os2


import android.util.Base64
import co.candyhouse.sesame.utils.*
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame.utils.hexStringToByteArray


import org.spongycastle.jce.ECNamedCurveTable
import org.spongycastle.jce.interfaces.ECPrivateKey
import org.spongycastle.jce.interfaces.ECPublicKey
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec
import org.spongycastle.jce.spec.ECPrivateKeySpec
import org.spongycastle.jce.spec.ECPublicKeySpec
import org.spongycastle.math.ec.ECPoint



import java.math.BigInteger
import java.security.Key
import java.security.KeyFactory
import java.security.Security
import java.security.spec.X509EncodedKeySpec

import javax.crypto.KeyAgreement


data class KeyQues(var ak: String, var n: String, var e: String, var t: Os2Type)
enum class Os2Type(val type: Int) {
    Bot(2),
    Bike(0),
    Sesame2(1);


}

data class KeyResp(
    var sig1: String,
    var st: String,
    var pubkey: String
)

object Os2CipherUtils {
    /*生成共享key*/
    internal fun ecdhShareKey(key: Key, remotePublicKey: ByteArray): ByteArray {
        val remoteECPub: ECPublicKey = KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec("3059301306072a8648ce3d020106082a8648ce3d030107034200".hexStringToByteArray() + remotePublicKey)) as ECPublicKey
        val kaA = KeyAgreement.getInstance("ECDH")
        kaA.init(key)
        kaA.doPhase(remoteECPub, true)
        return kaA.generateSecret()
    }



    const val serverKey =
        "04a040fcc7386b2a08304a3a2f0834df575c936794209729f0d42bd84218b35803932bea522200b2ebcbf17ab57c4509b4a3f1e268b2489eb3b75f7a765adbe181"


  fun getPublicKey(privateKeyHex: String?): Pair<ByteArray?,ECPrivateKey?> {
        Security.insertProviderAt(BouncyCastleProvider(), 1)
        try {
            val privateKeyValue = BigInteger(privateKeyHex, 16)
            val spec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("prime256v1")
            val privateKeySpec = ECPrivateKeySpec(privateKeyValue, spec)
            val keyFactory: KeyFactory = KeyFactory.getInstance("EC", "SC")
            val privateKey: ECPrivateKey =
                keyFactory.generatePrivate(privateKeySpec) as ECPrivateKey
            val w: ECPoint = spec.getG().multiply(privateKey.getD())
            val publicKeySpec = ECPublicKeySpec(w, spec)
            val publicKey: ECPublicKey = keyFactory.generatePublic(publicKeySpec) as ECPublicKey
            val publicKeyBytes: ByteArray = publicKey.getQ().getEncoded(false).drop(1).toByteArray()
            return   Pair(publicKeyBytes, privateKey)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(null, null)
    }
    fun getRegisterKey(data: KeyQues): KeyResp {


        val keyBytes = "Sesame2_key_pair".toByteArray()
        val erBytes: ByteArray = data.e.hexStringToByteArray()
        val oneKey = AesCmac(keyBytes, 16).computeMac(erBytes) //2b0a26d1b0c341b7670c4fa7ae01edf5
        val twoKey = AesCmac(
            oneKey!!,
            16
        ).computeMac(erBytes)
        val priKey=oneKey+twoKey!!
       val pair=getPublicKey(priKey.toHexString())
        var cutEccHeader = pair.first

        var ecdh = ecdhShareKey(pair.second!!, serverKey.hexStringToByteArray())
        var secret = ecdh.take(16).toByteArray()

        val serverToken=Base64.decode("XQDmrA==",Base64.DEFAULT)
        val stString = serverToken.base64Encode()
        var decode = Base64.decode(data.n, Base64.DEFAULT)
        val sessionToken = serverToken + decode

        var decodeToken = Base64.decode(data.ak, Base64.DEFAULT)
        val msg = decodeToken + sessionToken
        val sig1 = AesCmac(secret, 16).computeMac(msg)

        val sigString = sig1!!.slice(0..3).toByteArray().base64Encode()
        val pubString = cutEccHeader!!.base64Encode()


        return KeyResp(sigString, stString, pubString)
    }


}