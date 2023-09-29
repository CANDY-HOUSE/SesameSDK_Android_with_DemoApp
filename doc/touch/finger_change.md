# Finger Change 説明
1. ssm_touch 新しい指紋を追加すると、アプリに指紋情報が自動的にプッシュされます。
2. アプリで新しい名前を変更し、命令115をssm_touchに送信します。

### 送信フォーマット

|  Byte  |       N~1 |    0     |
|:------:|----------:|:--------:|
| Data   | payload |  command |

- command:命令115(固定)
- payload:payload表を参照する

### 受信フォーマット
| Byte  |        2   |     1     |     0      |
|:---:|:-----------:|:----:|:---------:|
| Data |  status | command |response   |
- command:命令115(固定)
- response:応答0x07(固定)
    - status:0x00(成功)
### プッシュフォーマット

| Byte  |          2 |     1     |  0   |
|:---:|:---:|----------:|:----:|
| Data | payload|   command | push |
- command:命令115(固定)
- push:応答0x08(固定)
- payload:payload表を参照する

##### **payload表**

|  Byte  |     findger_name| findger_name_length| findger_id|     0 |
|:------:|:---------:|:--------:|:--------:|:--------:|
| Data   | findger_name	 | findger_name_length |findger_id|findger_id_length|

### フローチャート
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
