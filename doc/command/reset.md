# Reset 重置设备

### 发送格式  

|  Byte  |      0       |
|:------:|:------------:|
| Data   |   command    |
 

- command:指令104(固定)


### 接收格式  

|  Byte   | 2 | 1 | 0 |    
|:-------:|:------:|:---------:|:----------:|  
|  Data   | status | command | response  |  
- command:指令104(固定)
- response:响应0x07(固定)
- status:状态0x00(成功)
### 循序图
![icon](reset.svg)





### android示例
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
