
# CHSesameBike 接口
```svg


interface CHSesameBike : CHSesameLock {
fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
}
```
`CHSesameBike` 是一个设备接口，继承自 `CHSesameLock` 接口。它负责管理和操作 Sesame Bike 锁设备。

## 方法

- `fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - 解锁设备。如果操作成功，则返回操作结果，否则返回错误信息。

