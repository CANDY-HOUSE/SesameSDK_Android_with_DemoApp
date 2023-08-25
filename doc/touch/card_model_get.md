# Card Mode Get 讲解
手机发送113指令，获取卡片当前新增或者验证模式
### 发送格式

|  Byte  |       0 |
|:------:|-------:|
| Data   |  command |

- command:指令113(固定)




### 接收格式

| Byte  |     3    |   2 |   1   |     0      |
|:---:|:-------:|:-----:|:----:|:-----:|
| Data | card_mode|  status | command |response   |
- command:指令113(固定)
- response:响应0x07(固定)
- status:0x00(成功)
- card_mode:0x00->验证模式，0x01->新增模式


### 循序图
![icon](card_model_get.svg)





### android示例
``` java
 override fun cards(result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_CARD_GET.value, byteArrayOf())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
```
