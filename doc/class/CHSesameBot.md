
# CHSesameBot 接口
```svg

interface CHSesameBot : CHSesameLock {
var mechSetting: CHSesameBotMechSettings?
fun updateSetting(setting: CHSesameBotMechSettings, historyTag: ByteArray? = null, result: CHResult<CHEmpty>)
fun toggle(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)
fun lock(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)
fun unlock(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)
fun click(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)
}
```
`CHSesameBot` 是一个设备接口，继承自 `CHSesameLock` 接口。这个接口专门用来管理和操作 Sesame Bot 设备。

## 属性

- `var mechSetting: CHSesameBotMechSettings?` - 用于存储 Sesame Bot 的机械设置。

## 方法

- `fun updateSetting(setting: CHSesameBotMechSettings, historyTag: ByteArray? = null, result: CHResult<CHEmpty>)` - 更新 Sesame Bot 的设置。

- `fun toggle(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)` - 切换 Sesame Bot 的状态。

- `fun lock(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)` - 锁定 Sesame Bot。

- `fun unlock(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)` - 解锁 Sesame Bot。

- `fun click(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)` - 模拟点击 Sesame Bot。
