# SesameOS3BleCipher クラスのドキュメント
```svg
internal class SesameOS3BleCipher(val name: String, private var sessionKey: ByteArray, private var sault: ByteArray) {
    var encryptCounter: Long = 0.toLong()//int32 4byte-> 010000 小端
    var decryptCounter: Long = 0.toLong()
    internal fun encrypt(plaintext: ByteArray): ByteArray
    internal fun decrypt(ciphertext: ByteArray): ByteArray 

}

```
`SesameOS3BleCipher` は、内部クラスであり、Sesame OS3 Ble デバイスのデータの暗号化と復号化を担当します。

## メンバー変数

- `val name: String`：デバイスの名前。

- `private var sessionKey: ByteArray`：セッションキー。

- `private var sault: ByteArray`：暗号化および復号化に使用される値。

- `var encryptCounter: Long`：暗号化カウンター。

- `var decryptCounter: Long`：復号化カウンター。

## メソッド

- `fun encrypt(plaintext: ByteArray): ByteArray`：このメソッドは、平文データのバイト配列を受け取り、セッションキーを使用して暗号化し、暗号化されたバイト配列を返します。

- `fun decrypt(ciphertext: ByteArray): ByteArray`：このメソッドは、暗号文データのバイト配列を受け取り、セッションキーを使用して復号化し、復号化されたバイト配列を返します。

暗号化および復号化のプロセスでは、このクラスは "AES/CCM/NoPadding" アルゴリズムと "BC" プロバイダーを使用します。Cipher オブジェクトの初期化時には GCM パラメータ仕様（GCMParameterSpec）が使用され、各暗号化および復号化のデータには追加の認証データ（AAD）が追加されます。
