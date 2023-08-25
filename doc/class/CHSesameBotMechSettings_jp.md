# CHSesameBotMechSettings データクラスのドキュメント
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

`CHSesameBotMechSettings` データクラスは、Sesame Bot 机械デバイスの設定を格納および管理するためのクラスです。

## プロパティ

- `userPrefDir`：ユーザーの優先方向を表す、バイト（Byte）型のプロパティです。
- `lockSec`：ロックする秒数を表す、バイト（Byte）型のプロパティです。
- `unlockSec`：ロック解除する秒数を表す、バイト（Byte）型のプロパティです。
- `clickLockSec`：クリックしてロックする秒数を表す、バイト（Byte）型のプロパティです。
- `clickHoldSec`：ボタンを長押しする秒数を表す、バイト（Byte）型のプロパティです。
- `clickUnlockSec`：クリックしてロックを解除する秒数を表す、バイト（Byte）型のプロパティです。
- `buttonMode`：ボタンのモードを表す、バイト（Byte）型のプロパティです。

## メソッド

- `data()`：このメソッドは、すべてのバイト（Byte）型のプロパティを結合し、その後ろに5つのゼロバイトを追加したByteArrayを返します。このメソッドは、デバイスの設定を送信または保存可能なデータ形式に変換するために使用されます。
