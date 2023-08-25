# Card Change 讲解
ssm_touch 加入新卡，会主动推送新卡ID和名称给手机
app修改卡片名称发送107指令
### 发送格式

|  Byte  |      N~1 |       0 |
|:------:|---------:|--------:|
| Data   | payload	 | command |

- command:指令107(固定)
- payload:见payload表格  
##### **payload如下**  

|  Byte  |     card_name| card_name_length| card_id|     0 |
|:------:|:---------:|:--------:|:--------:|:--------:|
| Data   | card_name	 | card_name_length |card_id|card_id_length|


### 接收格式

| Byte  |    2 |   1   |     0      |
|:---:|:----:|:----:|:-----:|
| Data |  status  | command |response   |
- command:指令107(固定)
- response:响应0x07(固定)
- status:状态0x00(成功)
### 推送信息
|  Byte  |   N~2    |    1    |    0     |
|:------:|:--------:|:--------:|:--------:|
| Data   | payload	 | command |response  |
- command:指令107(固定)
- response:响应0x08(固定)
- payload:见payload表格
##### **payload如下**

|  Byte  |     card_name| card_name_length| card_id|     0 |
|:------:|:---------:|:--------:|:--------:|:--------:|
| Data   | card_name	 | card_name_length |card_id|card_id_length|

### 循序图
![icon](card_change.svg)





### android示例
``` java
  override fun cardChange(ID: String, name: String, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_CARD_CHANGE.value, byteArrayOf(ID.hexStringToByteArray().size.toByte()) + ID.hexStringToByteArray() + name.toByteArray())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
```
