# Finger Mode Get 讲解
app 发送发送121指令，获取ssm_touch当前指纹状态
### 发送格式

|  Byte  |       0 |
|:------:|-------:|
| Data   |  command |

- command:指令121(固定)




### 接收格式

| Byte  |          3    | 2   |     1     |     0      |
|:---:|:-----------:|:------:|:----:|:---------:|
| Data | finger_mode | status | command |response   |
- command:指令121(固定)
- response:响应0x07(固定)
  - status:0x00(成功) 
  - finger_mode:0x00->验证模式，0x01->新增模式 



### 循序图
![icon](finger_mode_get.svg)





### android示例
``` java
  override fun fingerPrintModeGet(result: CHResult<Byte>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_FINGERPRINT_MODE_GET.value, byteArrayOf())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(res.payload[0])))
        }
    }
```
