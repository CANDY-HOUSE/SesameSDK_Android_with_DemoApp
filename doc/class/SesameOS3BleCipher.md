
# SesameOS3BleCipher 类
```svg
internal class SesameOS3BleCipher(val name: String, private var sessionKey: ByteArray, private var sault: ByteArray) {
    var encryptCounter: Long = 0.toLong()//int32 4byte-> 010000 小端
    var decryptCounter: Long = 0.toLong()
    internal fun encrypt(plaintext: ByteArray): ByteArray
    internal fun decrypt(ciphertext: ByteArray): ByteArray 

}

```
`SesameOS3BleCipher` 是一个内部类，它负责 Sesame OS3 Ble 设备的数据加密和解密。

## 成员变量

- `val name: String`: 设备的名称。

- `private var sessionKey: ByteArray`: 会话密钥。

- `private var sault: ByteArray`: 加密和解密所使用的值。

- `var encryptCounter: Long`: 加密计数器。

- `var decryptCounter: Long`: 解密计数器。

## 方法

- `fun encrypt(plaintext: ByteArray): ByteArray`: 这个方法接受一个明文数据的字节数组，然后用会话密钥进行加密，并返回加密后的字节数组。

- `fun decrypt(ciphertext: ByteArray): ByteArray`: 这个方法接受一个密文数据的字节数组，然后用会话密钥进行解密，并返回解密后的字节数组。

在加密和解密过程中，这个类使用了 "AES/CCM/NoPadding" 算法，以及 "BC" 提供者。在初始化 Cipher 对象时，使用了 GCM 参数规范（GCMParameterSpec），并且对每次加密和解密的数据都增加了额外的认证数据（AAD）。
