# KeyboardPassword Delete 説明
app は命令124を送信し、パスワードを削除します。
### 送信フォーマット

|  Byte  |     1 |    0     |
|:------:|------:|:--------:|
| Data   | pw_id |  command |

- command:命令124(固定)
- pc_id:指紋ID




### 受信フォーマット

| Byte  |        2   |     1     |     0      |
|:---:|:-----------:|:----:|:---------:|
| Data |  status | command |response   |
- command:命令124(固定)
- response:応答0x07(固定)
    - status:0x00(成功)




### フローチャート
![icon](kbpc_delete.svg)





### android例
``` java
   override fun keyBoardPassCodeDelete(ID: String, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_PASSCODE_DELETE.value, ID.hexStringToByteArray())) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
```
