# AutoLock自动锁

### 送信フォーマット
| バイト | 2〜1 | 0 |
|:----:|:-----:|:------:|
| データ | 遅延時間 | コマンド |
- コマンド: 指令11（固定）
- 遅延時間: 遅延時間（秒）、0は自動解錠無効

## 受信フォーマット
| バイト | 2 | 1 | 0 |
|:----:|:----:|:----:|:----:|
| データ | ステータス | コマンド | レスポンス |
- コマンド: 指令11（固定）
- レスポンス: 応答0x07（固定）
- ステータス: 状態0x00（成功）

## シーケンス図
![v](autolock.svg)

## Androidの例
``` java
    override fun autolock(delay: Int, result: CHResult<Int>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.autolock.value, delay.toShort().toReverseBytes()), DeviceSegmentType.cipher) { res ->
            mechSetting?.autoLockSecond = delay.toShort()
            result.invoke(Result.success(CHResultState.CHResultStateBLE(delay)))
        }
    }
