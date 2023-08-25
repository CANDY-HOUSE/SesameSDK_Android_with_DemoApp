# AutoLock自动锁

### 发送格式
| Byte | 2 ~ 1 | 0 |
|:----:|:-----:|:------:|
| Data | delay_duration|  command |
- command:指令11(固定)
- delay_duration:延迟时长(s) 0禁用自动开锁

## 接收格式
| Byte | 2 | 1 | 0 |
|:----:|:----:|:----:|:----:|
| Data | status | command | response  |
- response:响应0x07(固定)
- command:指令11(固定)
- status:状态0x00(成功) 
## 循序图
![v](autolock.svg)





## android示例
``` java
    override fun autolock(delay: Int, result: CHResult<Int>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.autolock.value, delay.toShort().toReverseBytes()), DeviceSegmentType.cipher) { res ->
            mechSetting?.autoLockSecond = delay.toShort()
            result.invoke(Result.success(CHResultState.CHResultStateBLE(delay)))
        }
    }
```
