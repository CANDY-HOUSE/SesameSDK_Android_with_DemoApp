
# CHSesameBotMechSettings 数据类
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
`CHSesameBotMechSettings`数据类主要用于存储和管理Sesame Bot机械设备的设置。

## 属性

- `userPrefDir`：用户偏好方向，Byte类型。
- `lockSec`：锁定秒数，Byte类型。
- `unlockSec`：解锁秒数，Byte类型。
- `clickLockSec`：点击锁定秒数，Byte类型。
- `clickHoldSec`：长按秒数，Byte类型。
- `clickUnlockSec`：点击解锁秒数，Byte类型。
- `buttonMode`：按钮模式，Byte类型。

## 方法

- `data()`：此方法将所有的Byte类型属性组合为一个ByteArray，并在其后添加五个字节的0。此方法主要用于将设备设置转换为可以发送或存储的数据格式。
