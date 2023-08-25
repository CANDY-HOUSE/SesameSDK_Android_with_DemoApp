# Finger Mode Set 説明
アプリが122コマンドを送信して、ssm_touchの現在の指紋状態を設定します。

### 送信フォーマット

|  バイト  |  1|    0    |
|:------:|----:|:-------:|
| データ   | 指紋モード| コマンド |

- コマンド: 指令122（固定）
- 指紋モード: 指紋のモード

### 受信フォーマット

| バイト  |          3    | 2   |     1     |     0      |
|:---:|:-----------:|:------:|:----:|:---------:|
| データ | 指紋モード | ステータス | コマンド |レスポンス   |
- コマンド: 指令122（固定）
- レスポンス: 応答0x07（固定）
  - ステータス: 0x00（成功）
  - 指紋モード: 0x00->認証モード、0x01->追加モード

### シーケンス図
![アイコン](finger_model_set.svg)

### Androidの例
```java
   override fun fingerPrintModeSet(mode: Byte, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_FINGERPRINT_MODE_SET.value, byteArrayOf(mode))) {
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
