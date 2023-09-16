package co.candyhouse.sesame.ble.os2


import android.util.Base64
import co.candyhouse.sesame.utils.*
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame.utils.hexStringToByteArray

import java.math.BigInteger
import java.security.*
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.*
import javax.crypto.KeyAgreement
import kotlin.random.Random


data class KeyQues(var ak: String, var n: String, var e: String)


data class KeyResp(
    var sig1: String,
    var st: String,
    var pubkey: String
)

object CHServerAuth {
    const val serverKey =
        "04a040fcc7386b2a08304a3a2f0834df575c936794209729f0d42bd84218b35803932bea522200b2ebcbf17ab57c4509b4a3f1e268b2489eb3b75f7a765adbe181"

    /*生成共享key*/
    private fun ecdhShareKey(key: Key, remotePublicKey: ByteArray): ByteArray {
        val remoteECPub: ECPublicKey = KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec("3059301306072a8648ce3d020106082a8648ce3d030107034200".hexStringToByteArray() + remotePublicKey)) as ECPublicKey
        val kaA = KeyAgreement.getInstance("ECDH")
        kaA.init(key)
        kaA.doPhase(remoteECPub, true)
        return kaA.generateSecret()
    }

    fun getRegisterKey(data: KeyQues): KeyResp {

        val keyBytes = "Sesame2_key_pair".toByteArray()
        val erBytes: ByteArray = data.e.hexStringToByteArray()
        val oneKey = AesCmac(keyBytes, 16).computeMac(erBytes) //2b0a26d1b0c341b7670c4fa7ae01edf5
        val twoKey = AesCmac(
            oneKey!!,
            16
        ).computeMac(erBytes)
        val priKey = oneKey + twoKey!!
        val pair = priKeyToPubKey(priKey.toHexString())
        var ecdh = ecdhShareKey(pair.second!!, serverKey.hexStringToByteArray())
        var secret = ecdh.take(16).toByteArray()
        val serverToken = ByteArray(4)
        Random.nextBytes(serverToken)
        val stString = serverToken.base64Encode()
        var decode = Base64.decode(data.n, Base64.DEFAULT)
        val sessionToken = serverToken + decode
        var decodeToken = Base64.decode(data.ak, Base64.DEFAULT)
        val msg = decodeToken + sessionToken
        val sig1 = AesCmac(secret, 16).computeMac(msg)
        val sigString = sig1!!.slice(0..3).toByteArray().base64Encode()
        val pubString = pair.first!!.base64Encode()
        return KeyResp(sigString, stString, pubString)
    }

    /* a->y² = x³ + ax + b
 *  p-> 私钥生成 FFFFFFFF 00000001 00000000 00000000 00000000 FFFFFFFF FFFFFFFF FFFFFFFF
 * k->要乘的数

 * field->素数域ECParameterSpec的getCurve().getField()
 * */
    private fun multiply(p: ECPoint, k: BigInteger, field: ECFieldFp, a: BigInteger?): ECPoint? {
        var p: ECPoint = p
        var k: BigInteger = k
        var q: ECPoint = ECPoint.POINT_INFINITY
        while (k.signum() > 0) {
            if (k.testBit(0)) {
                q = add(q, p, field, a)
            }
            p = doublePoint(p, field, a)
            k = k.shiftRight(1)
        }
        return q
    }

    private fun add(p1: ECPoint, p2: ECPoint, field: ECFieldFp, a: BigInteger?): ECPoint {
        if (p1 == ECPoint.POINT_INFINITY) return p2
        if (p2 == ECPoint.POINT_INFINITY) return p1
        val p: BigInteger = field.p
        val lambda: BigInteger = p2.affineY.subtract(p1.affineY)
            .multiply(p2.affineX.subtract(p1.affineX).modInverse(p)).mod(p)
        val x3: BigInteger =
            lambda.multiply(lambda).subtract(p1.affineX).subtract(p2.affineX).mod(p)
        val y3: BigInteger =
            p1.affineY.add(lambda.multiply(x3.subtract(p1.affineX))).negate().mod(p)
        return ECPoint(x3, y3)
    }

    private fun doublePoint(p: ECPoint, field: ECFieldFp, a: BigInteger?): ECPoint {
        if (p == ECPoint.POINT_INFINITY) return p
        val pValue: BigInteger = field.p
        val lambda: BigInteger = p.affineX.pow(2).multiply(BigInteger.valueOf(3)).add(a)
            .multiply(p.affineY.multiply(BigInteger.valueOf(2)).modInverse(pValue)).mod(pValue)
        val x3: BigInteger =
            lambda.pow(2).subtract(p.affineX.multiply(BigInteger.valueOf(2))).mod(pValue)
        val y3: BigInteger =
            p.affineY.add(lambda.multiply(x3.subtract(p.affineX))).negate().mod(pValue)
        return ECPoint(x3, y3)
    }


 private   fun priKeyToPubKey(privateKeyHex: String): Pair<ByteArray?, ECPrivateKey?> {
        try {
            val keySpec =
                PKCS8EncodedKeySpec("3041020100301306072a8648ce3d020106082a8648ce3d030107042730250201010420${privateKeyHex}".hexStringToByteArray())
            // 使用"EC"（椭圆曲线）算法创建KeyFactory对象
            val kf: KeyFactory = KeyFactory.getInstance("EC")
            // 生成私钥
            val privateKey: PrivateKey = kf.generatePrivate(keySpec)
            val ecPrivateKey: ECPrivateKey = privateKey as ECPrivateKey

            //  g((ECPrivateKey) privateKey);
            val params: ECParameterSpec = ecPrivateKey.params
            val field: ECFieldFp = params.curve.field as ECFieldFp
            val a: BigInteger = params.curve.a
            val G: ECPoint = params.generator
            val s: BigInteger = ecPrivateKey.s
            val ecPoint: ECPoint? = multiply(G, s, field, a)
            val publicKeySpec = ECPublicKeySpec(
                ecPoint,
                ecPrivateKey.params
            )
            val keyFactory: KeyFactory = KeyFactory.getInstance("EC")
            // 生成公钥
            val publicKey: PublicKey = keyFactory.generatePublic(publicKeySpec)

            return Pair(publicKey.encoded.drop(27).toByteArray(), privateKey)
        } catch (e: Exception) {

            e.printStackTrace()

        }

        return Pair(null, null)


    }

}