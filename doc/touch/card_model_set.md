# Card Mode Set 讲解
手机发送114指令，设置当前处于新增或者验证模式
### 发送格式

|  Byte  |  1|    0    |
|:------:|----:|:-------:|
| Data   | card_mode| command |

- command:指令114(固定)

- card_mode:0x00->验证模式，0x01->新增模式


### 接收格式

| Byte  |   3|    2    |   1   |     0      |
|:---:|:----:|:-------:|:-----:|:----:|
| Data | card_mode|  status | command |response   |
- command:指令114(固定)
- response:响应0x07(固定)
- status:0x00(成功)  
- card_mode:0x00->验证模式，0x01->新增模式

### 循序图
![icon](card_model_set.svg)





### android示例
``` java
  override fun cardModeSet(mode: Byte, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_CARD_MODE_SET.value, byteArrayOf(mode))) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
```
