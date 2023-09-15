# Reset リセット

### 送信フォーマット  

|  Byte  |      0       |
|:------:|:------------:|
| Data   |   command    |
 

- command:命令104(固定)


### 受信フォーマット  

|  Byte   | 2 | 1 | 0 |    
|:-------:|:------:|:---------:|:----------:|  
|  Data   | status | command | response  |  
- command:命令104(固定)
- response:応答0x07(固定)
- status:状態0x00(成功)
### フローチャート
![icon](reset.svg)





### android例
``` java
        open fun reset(result: CHResult<CHEmpty>) {
        sendCommand(SesameOS3Payload(SesameItemCode.Reset.value, byteArrayOf()), DeviceSegmentType.cipher) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                dropKey(result)
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }
```
