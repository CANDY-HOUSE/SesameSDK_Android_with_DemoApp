# Finger Get 説明
携帯電話で命令117を送信し、指紋情報を取得します。
### 送信フォーマット

|  Byte  |       0 |
|:------:|-------:|
| Data   |  command |
- command:命令117(固定)

### 受信フォーマット
| Byte  |       2 |   1   |     0      |
|:---:|:-------:|:-----:|:----:|
| Data |  status | command |response   |
- command:命令117(固定)
- response:応答0x07(固定)
- status:0x00(成功) 
### プッシュフォーマット-start
| Byte  |       2 |   1   |  0   |
|:---:|:-------:|:-----:|:----:|
| Data |  status | command | push |
- command:命令120(固定)
- response:応答0x08(固定)
- push:0x00(成功)
### プッシュフォーマット
| Byte  | N~   2 |   1   |  0   |
|:---:|:------:|:-----:|:----:|
| Data | payload | command | push |
- command:命令118(固定)
- push:応答0x08(固定)
- payload:playload表を参照する.

##### **payload表**

|  Byte  |     finger_name| finger_name_length| finger_id|     0 |
|:------:|:---------:|:--------:|:--------:|:--------:|
| Data   | finger_name     | finger_name_length |finger_id|finger_id_length|
### プッシュフォーマット-end
| Byte  |       2 |   1   |     0      |
|:---:|:-------:|:-----:|:----:|
| Data |  status | command |push   |
- command:命令119(固定)
- response:応答0x08(固定)
- push:0x00(成功)


### フローチャート
![icon](finger_get.svg)





### android例
``` java
  override fun fingerPrints(result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_FINGERPRINT_GET.value, byteArrayOf())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
```
