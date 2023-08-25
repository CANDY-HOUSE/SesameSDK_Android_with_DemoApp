# Card Mode Set の説明
スマートフォンは114コマンドを送信して、現在のモードを設定します。

### 送信形式

| byte |  1|    0    |
|:----:|----:|:-------:|
| Data | card_mode| command |

- command: 114コマンド (固定)
- card_mode: 0x00->認証モード、0x01->追加モード

### 受信形式

| byte  |   3|    2    |   1   |     0      |
|:----:|:----:|:-------:|:-----:|:----:|
| Data | card_mode|  status | command |response   |
- command: 114コマンド (固定)
- response: 0x07応答 (固定)
- status: 状態 0x00 (成功)
- card_mode: 0x00->認証モード、0x01->追加モード

### シーケンス図
![icon](card_model_set.svg)

### Androidの例
``` java
  override fun cardModeSet(mode: Byte, result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_CARD_MODE_SET.value, byteArrayOf(mode))) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
