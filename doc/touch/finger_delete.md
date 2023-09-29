# Finger Delete 説明
アプリから命令116を送信して、ID指紋を削除します。
### 送信フォーマット

|  Byte  | 1|    0     |
|:------:|---:|:--------:|
| Data   | finger_id|  command |

- command:命令116(固定)
- finger_id:指紋ID




### 受信フォーマット

| Byte  |        2   |     1     |     0      |
|:---:|:-----------:|:----:|:---------:|
| Data |  status | command |response   |
- command:命令116(固定)
- response:応答0x07(固定)
    - status:0x00(成功)




### フローチャート
![icon](finger_delete.svg)





### android例
``` java
 override fun fingerPrintDelete(ID: String, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_FINGERPRINT_DELETE.value, ID.hexStringToByteArray())) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
```
