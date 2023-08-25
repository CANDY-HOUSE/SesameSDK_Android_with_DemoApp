
# CHSesameSensor 接口
```svg

interface CHSesameSensor : CHSesameLock, CHSesameConnector {
fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun deleteCards(cardID: String, result: CHResult<CHEmpty>)
fun getCards(result: CHResult<ArrayList<String>>)
}
```

`CHSesameSensor` 是一个设备接口，继承自 `CHSesameLock` 和 `CHSesameConnector` 接口。这个接口专门用来管理和操作 Sesame Sensor 设备。

## 方法

- `fun unlock(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)` - 解锁 Sesame Sensor。

- `fun deleteCards(cardID: String, result: CHResult<CHEmpty>)` - 删除指定的卡片。

- `fun getCards(result: CHResult<ArrayList<String>>)` - 获取所有的卡片。

以上是 `CHSesameSensor` 接口的基本描述，这个接口为 Sesame Sensor 设备提供了一套完整的操作和管理方法。
