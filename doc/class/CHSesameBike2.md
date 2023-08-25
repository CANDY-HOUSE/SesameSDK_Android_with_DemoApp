
# CHSesameBike2 接口
```svg

interface CHSesameBike2 : CHSesameLock {

fun unlock(tag: ByteArray? = null, result: CHResult<CHEmpty>)
}

```
`CHSesameBike2` 是一个设备接口，继承自 `CHSesameLock` 接口。这个接口专门用来管理和操作 Sesame Bike2 锁设备。

## 方法

- `fun unlock(tag: ByteArray? = null, result: CHResult<CHEmpty>)` - 解锁设备。如果操作成功，则返回操作结果，否则返回错误信息。

