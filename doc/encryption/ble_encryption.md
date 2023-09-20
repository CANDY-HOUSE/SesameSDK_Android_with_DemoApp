# Ble 暗号化アルゴリズム
### ECDH　EccKeyという実装クラスは、楕円曲線暗号学の鍵交換プロトコルに基づき、プロジェクトでは楕円曲線の方程式「y^2 = x^3 - 3x + b」を使用して鍵ペアを生成し、アプリとBleデバイス間で暗号鍵を交換するために使用されます。AesCmacの鍵にも使用されます。


#### コード例
```agsl
AppはBleデバイスに公開鍵を送信する。
sendCommand(SesameOS3Payload(SesameItemCode.registration.value, EccKey.getPubK().hexStringToByteArray() + System.currentTimeMillis().toUInt32ByteArray()), DeviceSegmentType.plain) 
 共有鍵を生成する
 val ecdhSecret = EccKey.ecdh(eccPublicKeyFromSS5)
```
### AESCMAC　AesCmacはMacを継承した実装クラスです。computeMacメソッドを使用して、メッセージの認証コードを計算します。EccKeyの鍵にも使用されます。

#### コード例

```agsl
  AesCmac(secret, 16).computeMac(signPayload)
  
```
### AESCCM SesameOS3BleCipherという実装クラスは主にBluetooth通信の転送に使用されます。対称鍵暗号化とメッセージ認証コード（MAC）を組み合わせて、データの機密性と完全性の保護を実現します。プロジェクトで使用されたAESCCMはBLE通信がsessionAuthに対し暗号化と復号化の動作を行うことによって、生成されます。

#### コード例

```agsl
作成 
cipher = SesameOS3BleCipher( sessionAuth, ("00" + mSesameToken.toHexString()).hexStringToByteArray())
暗号化 
cipher.encrypt(payload.toDataWithHeader())
複合化
cipher.decrypt(ssmSay.second)
```
#### アルゴリズムのアプリケーション図
![ecdhアルゴリズム図](ble_encryption.svg)



