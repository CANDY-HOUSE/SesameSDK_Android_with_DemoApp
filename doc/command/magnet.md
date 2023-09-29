# Magnet 角度補正

### 送信フォーマット
| Byte | 0 |
|:----:|:----:|
| Data |   command |
- command:命令17(固定)


### 受信フォーマット
| Byte | 2 | 1 | 0 |
|:----:|:----:|:----:|:----:|
| Data | status | command | response  |
- command:命令17(固定)
- response:応答0x07(固定)
- status:状態0x00(成功) 
### フローチャート
![v](magnet.svg)





### android例
``` java
       override fun magnet(result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.magnet.value, byteArrayOf()), DeviceSegmentType.cipher) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
```
