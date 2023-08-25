# KeyboardPassword Mode Set 説明
アプリが130コマンドを送信して、ssm_touchの現在の数字ロックの状態を設定します。

### 送信フォーマット

|  バイト  |       1 |    0    |
|:------:|--------:|:-------:|
| データ   | 数字ロックモード | コマンド |

- コマンド: 指令130（固定）
- 数字ロックモード: 数字ロックモード

### 受信フォーマット

| バイト  |    3    | 2   |     1     |     0      |
|:---:|:-------:|:------:|:----:|:---------:|
| データ | 数字ロックモード | ステータス | コマンド |レスポンス   |
- コマンド: 指令130（固定）
- レスポンス: 応答0x07（固定）
  - ステータス: 0x00（成功）
  - 数字ロックモード: 0x00->認証モード，0x01->新規追加モード

### シーケンス図
![アイコン](kbpc_model_set.svg)

### Androidの例
```java
  override fun keyBoardPassCodeModeSet(mode: Byte, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_PASSCODE_MODE_SET.value, byteArrayOf(mode))) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
