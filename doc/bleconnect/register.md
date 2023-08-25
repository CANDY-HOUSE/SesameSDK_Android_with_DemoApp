# register 注册连接Ble

### 发送格式

|  Byte  | 68~65|        4~1 |       0 |  
|:------:|:----:|-----------:|--------:|
| Data   |timestamp|  publicKeyA | command |

- command:指令1(固定)
- timestamp:手机当前时间戳
- publicKeyA:由椭圆曲线（Elliptic Curve，EC）密钥对生成算法，属于非堆成加密算法
```agsl

 internal fun getDeviceECCKey(): KeyPair {
        keyPairA?.let { return it }
        val keyGen = KeyPairGenerator.getInstance("EC")
        keyGen.initialize(ECGenParameterSpec("secp256r1")) //prime256v1 == secp256r1 == NIST P-256
        val newKeyPairA = keyGen.generateKeyPair()
        keyPairA = newKeyPairA
        return newKeyPairA
    }
```

### 接收格式

| Byte  |79~16|15~10 |9~3|    2 |   1   |     0      |  
|:---:|:----:|:----:|:-----:|:----:|:-----:|:-----:|
| Data | publicKeyS|mechSetting|mechStatus| status  | command |response   |
- command:指令1(固定)
- response:响应0x07(固定)
- status:状态0x00(成功)  状态0x09(已注册)走[login](login.md)流程
- mechSetting:设备开关锁和开关锁角度
- mechStatus:机器当前各类信息状态
- publicKeyS:机器当前各类信息状态

### 循序图
![icon](register.svg)





### android示例
``` java
       override fun register(result: CHResult<CHEmpty>) {
        if (deviceStatus != CHDeviceStatus.ReadyToRegister) {
            result.invoke(Result.failure(NSError("Busy", "CBCentralManager", 7)))
            return
        }
        deviceStatus = CHDeviceStatus.Registering

        L.d("hcia", "register:!!")
        makeApiCall(result) {
            val serverSecret = mSesameToken.toHexString()
            sendCommand(SesameOS3Payload(SesameItemCode.registration.value, EccKey.getPubK().hexStringToByteArray() + System.currentTimeMillis().toUInt32ByteArray()), DeviceSegmentType.plain) { IRRes ->
                mechStatus = CHSesame5MechStatus(IRRes.payload.toHexString().hexStringToByteArray().sliceArray(0..6))
                mechSetting = CHSesame5MechSettings(IRRes.payload.toHexString().hexStringToByteArray().sliceArray(7..12))

                val eccPublicKeyFromSS5 = IRRes.payload.toHexString().hexStringToByteArray().sliceArray(13..76)
                val ecdhSecret = EccKey.ecdh(eccPublicKeyFromSS5)
                val ecdhSecretPre16 = ecdhSecret.sliceArray(0..15)
                val deviceSecret = ecdhSecretPre16.toHexString()
                val candyDevice = CHDevice(deviceId.toString(), advertisement!!.productModel!!.deviceModel(), null, "0000", deviceSecret, serverSecret)
                sesame2KeyData = candyDevice

                val sessionAuth = AesCmac(ecdhSecretPre16, 16).computeMac(mSesameToken)

                cipher = SesameOS3BleCipher("customDeviceName", sessionAuth!!, ("00" + mSesameToken.toHexString()).hexStringToByteArray())
                CHDB.CHSS2Model.insert(candyDevice) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                }

                deviceStatus = if (mechStatus?.isInLockRange == true) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked

            }
        }
    }
```
