# login 登录讲解

### 发送格式

|  Byte  |     4~1     |        0 |
|:------:|:-----------:|---------:|
| Data   | ccmkey 	 |  command |

- ccmkey :由[RandomCode](RandomCode.md)获取
- command:指令2(固定)


### 接收格式
| Byte |6~3| 2 | 1 | 0 |  
|-------|:------:|:------:|:------:|:------:|
| Data | timestamp|status | command | response  |
- command:指令2(固定)
- response:响应0x07(固定)
- status:状态0x00(成功) 
- timestamp:时间戳
### 循序图
![login](login.svg)





### android示例
``` java
     override fun login(token: String?) {
        deviceStatus = CHDeviceStatus.BleLogining
        val sessionAuth: ByteArray? = AesCmac(sesame2KeyData!!.secretKey.hexStringToByteArray(), 16).computeMac(mSesameToken)

        cipher = SesameOS3BleCipher("customDeviceName", sessionAuth!!, ("00" + mSesameToken.toHexString()).hexStringToByteArray())
        sendCommand(SesameOS3Payload(SesameItemCode.login.value, sessionAuth!!.sliceArray(0..3)), DeviceSegmentType.plain) { loginPayload ->
            val systemTime = loginPayload.payload.sliceArray(0..3).toBigLong()
            val currentTimestamp = System.currentTimeMillis() / 1000
            val timeMinus = currentTimestamp.minus(systemTime)

            if (PreferenceManager.getDefaultSharedPreferences(CHBleManager.appContext).getString("nickname", "")?.contains(BuildConfig.testname) == true) {
                deviceTimestamp = systemTime
                loginTimestamp = currentTimestamp
            } else {
                if (abs(timeMinus) > 3) {
                    sendCommand(SesameOS3Payload(SesameItemCode.time.value, System.currentTimeMillis().toUInt32ByteArray()), DeviceSegmentType.cipher) {}
                }
            }
        }
    }
```
