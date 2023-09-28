# KeyboardPassword Mode Set 説明
app は命令130を送信し、ssm_touchの暗証番号の状態を設置します。
### 送信フォーマット

|  Byte  |       1 |    0    |
|:------:|--------:|:-------:|
| Data   | pw_mode | command |

- command:命令130(固定)
- pw_mode:暗証番号モード




### 受信フォーマット

| Byte  |    3    | 2   |     1     |     0      |
|:---:|:-------:|:------:|:----:|:---------:|
| Data | pw_mode | status | command |response   |
- command:命令130(固定)
- response:応答0x07(固定)
    - status:0x00(成功)
    - pw_mode:0x00->認証モード，0x01->新規モード



### フローチャート
![icon](kbpc_model_set.svg)





### android例
``` java
  override fun keyBoardPassCodeModeSet(mode: Byte, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_PASSCODE_MODE_SET.value, byteArrayOf(mode))) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

```
