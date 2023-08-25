# Magnet 角度矫正

### 发送格式
| Byte | 0 |
|:----:|:----:|
| Data |   command |
- command:指令17(固定)


### 接收格式
| Byte | 2 | 1 | 0 |
|:----:|:----:|:----:|:----:|
| Data | status | command | response  |
- command:指令17(固定)
- response:响应0x07(固定)
- status:状态0x00(成功) 
### 循序图
![v](magnet.svg)





### android示例
``` java
       override fun magnet(result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.magnet.value, byteArrayOf()), DeviceSegmentType.cipher) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
```
