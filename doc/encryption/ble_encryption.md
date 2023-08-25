# Ble 加密算法
### ECDH 实现类 EccKey 。基于椭圆曲线密码学的密钥交换协议，项目采用椭圆曲线方程：y^2 = x^3 - 3x + b 生成密钥对，用于APP和Ble设备交换加密密钥。用于AesCmac的密钥


#### 代码示例
```agsl
App发送公钥给Ble设备 
sendCommand(SesameOS3Payload(SesameItemCode.registration.value, EccKey.getPubK().hexStringToByteArray() + System.currentTimeMillis().toUInt32ByteArray()), DeviceSegmentType.plain) 
 生成共享密钥
 val ecdhSecret = EccKey.ecdh(eccPublicKeyFromSS5)
```
### AESCMAC 实现类 AesCmac ,继承Mac。用于计算消息的认证码，computeMac方法会生成消息认证码。用于EccKey的密钥

#### 代码示例

```agsl
  AesCmac(secret, 16).computeMac(signPayload)
  
```
### AESCCM 实现类 SesameOS3BleCipher,主要应用于蓝牙通讯传输，将对称加密和消息认证码（MAC）结合在一起，以实现数据的机密性和完整性保护。项目中采用AESCCM主要为BLE通讯加密解密sessionAuth来源AESCMAC生成
#### 代码示例

```agsl
创建 
cipher = SesameOS3BleCipher( sessionAuth, ("00" + mSesameToken.toHexString()).hexStringToByteArray())
加密 
cipher.encrypt(payload.toDataWithHeader())
解密
cipher.decrypt(ssmSay.second)
```
#### 算法应用图
![ecdh算法图](ble_encryption.svg)



