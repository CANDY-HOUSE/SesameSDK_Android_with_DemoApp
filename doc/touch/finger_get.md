# Finger Get 讲解
手机发送117指令，获取指纹信息
### 发送格式

|  Byte  |       0 |
|:------:|-------:|
| Data   |  command |
- command:指令117(固定)

### 接收格式
| Byte  |       2 |   1   |     0      |
|:---:|:-------:|:-----:|:----:|
| Data |  status | command |response   |
- command:指令117(固定)
- response:响应0x07(固定)
- status:0x00(成功) 
### 推送格式-start
| Byte  |       2 |   1   |  0   |
|:---:|:-------:|:-----:|:----:|
| Data |  status | command | push |
- command:指令120(固定)
- response:响应0x08(固定)
- push:0x00(成功)
### 推送格式
| Byte  | N~   2 |   1   |  0   |
|:---:|:------:|:-----:|:----:|
| Data | payload | command | push |
- command:指令118(固定)
- push:响应0x08(固定)
- payload:见 playload.

##### **payload如下**

|  Byte  |     finger_name| finger_name_length| finger_id|     0 |
|:------:|:---------:|:--------:|:--------:|:--------:|
| Data   | finger_name     | finger_name_length |finger_id|finger_id_length|
### 推送格式-end
| Byte  |       2 |   1   |     0      |
|:---:|:-------:|:-----:|:----:|
| Data |  status | command |push   |
- command:指令119(固定)
- response:响应0x08(固定)
- push:0x00(成功)


### 循序图
![icon](finger_get.svg)





### android示例
``` java
  override fun fingerPrints(result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_FINGERPRINT_GET.value, byteArrayOf())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
```
