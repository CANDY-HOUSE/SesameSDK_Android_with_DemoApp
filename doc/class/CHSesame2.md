

# CHSesame2 接口
```svg
interface CHSesame2 : CHSesameLock { // CHProductModel.SS2,CHProductModel.SS4
var mechSetting: CHSesame2MechSettings?
fun lock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun toggle(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun configureLockPosition(lockTarget: Short, unlockTarget: Short, result: CHResult<CHEmpty>)
fun getAutolockSetting(result: CHResult<Int>)
fun enableAutolock(delay: Int, historytag: ByteArray? = null, result: CHResult<Int>)
fun disableAutolock(historytag: ByteArray? = null, result: CHResult<Int>)
fun getHistories(cursor: Long?, result: CHResult<Pair<List<CHSesame2History>, Long?>>)
}

```
`CHSesame2` 是一个设备接口，继承自 `CHSesameLock` 接口。它负责管理和操作 Sesame2 锁设备。

## 属性

- `mechSetting: CHSesame2MechSettings?` - 获取或设置 Sesame2 的机械设置。

## 方法

- `fun lock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - 锁定设备。如果操作成功，则返回操作结果，否则返回错误信息。
- `fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - 解锁设备。如果操作成功，则返回操作结果，否则返回错误信息。
- `fun toggle(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - 切换设备的锁定状态。如果操作成功，则返回操作结果，否则返回错误信息。
- `fun configureLockPosition(lockTarget: Short, unlockTarget: Short, result: CHResult<CHEmpty>)` - 配置锁的位置。如果操作成功，则返回操作结果，否则返回错误信息。
- `fun getAutolockSetting(result: CHResult<Int>)` - 获取自动锁定的设置。如果操作成功，则返回操作结果，否则返回错误信息。
- `fun enableAutolock(delay: Int, historytag: ByteArray? = null, result: CHResult<Int>)` - 启用自动锁定。如果操作成功，则返回操作结果，否则返回错误信息。
- `fun disableAutolock(historytag: ByteArray? = null, result: CHResult<Int>)` - 禁用自动锁定。如果操作成功，则返回操作结果，否则返回错误信息。
- `fun getHistories(cursor: Long?, result: CHResult<Pair<List<CHSesame2History>, Long?>>)` - 获取设备的历史记录。如果操作成功，则返回操作结果，否则返回错误信息。

