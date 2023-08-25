# Lock 关锁

### 发送格式
| Byte | 7 ~ 1 | 0 |
|:----:|:----:|:----:|
| Data | historyTag|  command |
- command:指令82(固定)
- historyTag:历史标签

### 接收格式
| Byte | 2 | 1 | 0 |
|:----:|:----:|:----:|:----:|
| Data | status | command | response  |
- command:指令82(固定)
- response:响应0x07(固定)
- status:状态0x00(成功) 
### 循序图
![v](lock.svg)





### android示例
``` java
     override fun lock(historytag: ByteArray?, result: CHResult<CHEmpty>) {
        if (deviceStatus.value == CHDeviceLoginStatus.UnLogin && isConnectedByWM2) {
            CHAccountManager.cmdSesame(SesameItemCode.lock, this, sesame2KeyData!!.hisTagC(historytag), result)
        } else {
            if (checkBle(result)) return
//        L.d("hcia", "[ss5][lock] historyTag:" + sesame2KeyData!!.createHistagV2(historyTag).toHexString())
            sendCommand(SesameOS3Payload(SesameItemCode.lock.value, sesame2KeyData!!.createHistagV2(historytag)), DeviceSegmentType.cipher) { res ->
                if (res.cmdResultCode == SesameResultCode.success.value) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                } else {
                    result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
                }
            }
        }

    }
```
