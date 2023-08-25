# timestamp 更新时间戳

### 发送格式

|  Byte  | 4~1|       0 |  
|:------:|:----:|--------:|
| Data   | timestamp| command |

- command:指令8(固定)
- timestamp:手机当前时间戳



### 接收格式

| Byte  |    2    |   1   |     0      |  
|:---:|:-------:|:-----:|:---------:|
| Data | status  | command |response   |
- command:指令8(固定)
- response:响应0x07(固定)
- status:状态0x00(成功)

### 循序图
![icon](timestamp.svg)





### android示例
``` java
        override fun updateTime(result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.time.value, System.currentTimeMillis().toUInt32ByteArray()), DeviceSegmentType.cipher) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
```
