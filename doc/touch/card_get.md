# Card Get 讲解
手机发送109指令，获取当前卡片信息
### 发送格式

|  Byte  |     16~1 |       0 |
|:------:|---------:|--------:|
| Data   | card_id	 | command |

- command:指令109(固定)
- card_id:卡片ID



### 接收格式

| Byte  |    2 |   1   |     0      |
|:---:|:----:|:----:|:-----:|
| Data |  status  | command |response   |
- command:指令109(固定)
- response:响应0x07(固定)
- status:状态0x00(成功)  
### 推送格式-start
| Byte  |       2 |   1   |  0   |
|:---:|:-------:|:-----:|:----:|
| Data |  status | command | push |
- command:指令112(固定)
- response:响应0x08(固定)
- push:0x00(成功)
### 推送格式
| Byte  | N~   2 |   1   |  0   |
|:---:|:------:|:-----:|:----:|
| Data | payload | command | push |
- command:指令110(固定)
- push:响应0x08(固定)
- payload:见 playload.

##### **payload如下**

|  Byte  |     card_name| card_name_length| card_id|     0 |
|:------:|:---------:|:--------:|:--------:|:--------:|
| Data   | card_name     | card_name_length |card_id|card_id_length|
### 推送格式-end
| Byte  |       2 |   1   |     0      |
|:---:|:-------:|:-----:|:----:|
| Data |  status | command |push   |
- command:指令111(固定)
- response:响应0x08(固定)
- push:0x00(成功)

### 循序图
![icon](card_get.svg)





### android示例
``` java
 override fun cards(result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_CARD_GET.value, byteArrayOf())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
```
