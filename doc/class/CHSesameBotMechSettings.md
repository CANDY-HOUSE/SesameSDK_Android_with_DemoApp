
# CHSesameBotMechSettings データクラス
```svg

data class CHSesameBotMechSettings(
var userPrefDir: Byte,
var lockSec: Byte,
var unlockSec: Byte,
var clickLockSec: Byte,
var clickHoldSec: Byte,
var clickUnlockSec: Byte,
var buttonMode: Byte
) {
internal fun data(): ByteArray =
byteArrayOf(userPrefDir, lockSec, unlockSec, clickLockSec, clickHoldSec, clickUnlockSec, buttonMode) +
byteArrayOf(0, 0, 0, 0, 0)
}

```
`CHSesameBotMechSettings`データクラスはSesame Bot機器の設定を保存と管理します。

## プロパティ

- `userPrefDir`：ユーザーの好みや優先する方向を表し、Byte型です。
- `lockSec`：ロックの秒数を表します。Byte型です。
- `unlockSec`：解錠の秒数を表します。Byte型です。
- `clickLockSec`：ロックの秒数をクリックします。Byte型です。
- `clickHoldSec`：秒数を長押しします。Byte型です。
- `clickUnlockSec`：解錠の秒数をクリックします。Byte型です。
- `buttonMode`：ボタンモードです。Byte型です。

## メソッド

- `data()`：指定されたすべてのByte型のプロパティを1つのByteArrayに結合し、その後に5バイトの0を追加するメソッドです。デバイスの設定を送信または保存可能なデータ形式に変換します。
