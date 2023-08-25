
# CHSesame5 接口

```svg

interface CHSesame5 : CHSesameLock {
var mechSetting: CHSesame5MechSettings?
fun lock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun toggle(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun magnet(result: CHResult<CHEmpty>)
fun configureLockPosition(lockTarget: Short, unlockTarget: Short, result: CHResult<CHEmpty>)
fun autolock(delay: Int, result: CHResult<Int>)
fun history(cursor: Long?, uuid: UUID, result: CHResult<Pair<List<CHSesame5History>, Long?>>)
}
```

`CHSesame5` 是一个设备接口，继承自 `CHSesameLock` 接口。它负责管理和操作 Sesame5 锁设备。

## 属性

- `mechSetting: CHSesame5MechSettings?` - 获取或设置 Sesame5 的机械设置。

## 方法

- `fun lock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - 锁定设备。如果操作成功，则返回操作结果，否则返回错误信息。
- `fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - 解锁设备。如果操作成功，则返回操作结果，否则返回错误信息。
- `fun toggle(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - 切换设备的锁定状态。如果操作成功，则返回操作结果，否则返回错误信息。
- `fun magnet(result: CHResult<CHEmpty>)` - 对设备进行磁化操作。如果操作成功，则返回操作结果，否则返回错误信息。
- `fun configureLockPosition(lockTarget: Short, unlockTarget: Short, result: CHResult<CHEmpty>)` - 配置锁的位置。如果操作成功，则返回操作结果，否则返回错误信息。
- `fun autolock(delay: Int, result: CHResult<Int>)` - 设置自动锁定的延迟时间。如果操作成功，则返回操作结果，否则返回错误信息。
- `fun history(cursor: Long?, uuid: UUID, result: CHResult<Pair<List<CHSesame5History>, Long?>>)` - 获取设备的历史记录。如果操作成功，则返回操作结果，否则返回错误信息。

