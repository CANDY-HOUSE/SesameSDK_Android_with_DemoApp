# Finger Change 讲解
1. ssm_touch 添加新指纹，会主动推送指纹信息给APP
2. APP修改新成名称发送115给ssm_touch

### 发送格式

|  Byte  |       N~1 |    0     |
|:------:|----------:|:--------:|
| Data   | payload |  command |

- command:指令115(固定)
- payload:见payload表格

### 接收格式
| Byte  |        2   |     1     |     0      |
|:---:|:-----------:|:----:|:---------:|
| Data |  status | command |response   |
- command:指令115(固定)
- response:响应0x07(固定)
    - status:0x00(成功)
### 推送格式

| Byte  |          2 |     1     |  0   |
|:---:|:---:|----------:|:----:|
| Data | payload|   command | push |
- command:指令115(固定)
- push:响应0x08(固定)
- payload:见payload表格

##### **payload如下**

|  Byte  |     findger_name| findger_name_length| findger_id|     0 |
|:------:|:---------:|:--------:|:--------:|:--------:|
| Data   | findger_name	 | findger_name_length |findger_id|findger_id_length|

### 循序图
![icon](finger_change.svg)





### android示例
``` java
   override fun fingerPrintsChange(ID: String, name: String, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_FINGERPRINT_CHANGE.value, byteArrayOf(ID.hexStringToByteArray().size.toByte()) + ID.hexStringToByteArray() + name.toByteArray())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
```
