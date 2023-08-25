# KeyboardPassword  Get 讲解
手机发送125指令，获取ssm_touch密码
### 发送格式

|  Byte  |       0 |
|:------:|-------:|
| Data   |  command |

- command:指令125(固定)

### 接收格式

| Byte  |       2 |   1   |     0      |
|:---:|:-------:|:-----:|:----:|
| Data |  status | command |response   |
- command:指令125(固定)
- response:响应0x07(固定)
- status:0x00(成功)
### 推送格式-start
| Byte  |       2 |   1   |  0   |
|:---:|:-------:|:-----:|:----:|
| Data |  status | command | push |
- command:指令127(固定)
- response:响应0x08(固定)
- push:0x00(成功)
### 推送格式
| Byte  | N~   2 |   1   |  0   |
|:---:|:------:|:-----:|:----:|
| Data | payload | command | push |
- command:指令125(固定)
- push:响应0x08(固定)
- payload:见 playload.

##### **payload如下**

|  Byte  |     pw_name| pw_name_length| pw_id|     0 |
|:------:|:---------:|:--------:|:--------:|:--------:|
| Data   | pw_name     | pw_name_length |pw_id|pw_id_length|
### 推送格式-end
| Byte  |       2 |   1   |     0      |
|:---:|:-------:|:-----:|:----:|
| Data |  status | command |push   |
- command:指令128(固定)
- response:响应0x08(固定)
- push:0x00(成功)
### 循序图
![icon](kbpc_get.svg)





### android示例
``` java
  override fun keyBoardPassCode(result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_PASSCODE_GET.value, byteArrayOf())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
```
