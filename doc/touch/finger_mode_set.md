# Finger Mode Set 讲解
app 发送发送122指令，设置ssm_touch当前指纹状态
### 发送格式

|  Byte  |  1|    0    |
|:------:|----:|:-------:|
| Data   | finger_mode| command |

- command:指令122(固定)
- finger_mode:指纹模式




### 接收格式

| Byte  |          3    | 2   |     1     |     0      |
|:---:|:-----------:|:------:|:----:|:---------:|
| Data | finger_mode | status | command |response   |
- command:指令122(固定)
- response:响应0x07(固定)
  - status:0x00(成功) 
  - finger_mode:0x00->验证模式，0x01->新增模式 



### 循序图
![icon](finger_model_set.svg)





### android示例
``` java
   override fun fingerPrintModeSet(mode: Byte, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_FINGERPRINT_MODE_SET.value, byteArrayOf(mode))) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

```
