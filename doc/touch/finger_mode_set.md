# Finger Mode Set 説明
app は命令122を送信し、ssm_touchの指紋状態を設置します。
### 送信フォーマット

|  Byte  |  1|    0    |
|:------:|----:|:-------:|
| Data   | finger_mode| command |

- command:命令122(固定)
- finger_mode:指纹モード




### 受信フォーマット

| Byte  |          3    | 2   |     1     |     0      |
|:---:|:-----------:|:------:|:----:|:---------:|
| Data | finger_mode | status | command |response   |
- command:命令122(固定)
- response:応答0x07(固定)
  - status:0x00(成功) 
  - finger_mode:0x00->認証モード，0x01->新規モード 



### フローチャート
![icon](finger_model_set.svg)





### android例
``` java
   override fun fingerPrintModeSet(mode: Byte, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_FINGERPRINT_MODE_SET.value, byteArrayOf(mode))) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

```
