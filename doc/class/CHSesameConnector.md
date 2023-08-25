# CHSesameConnector 接口
```svg


interface CHSesameConnector : CHDevices {
    var ssm2KeysMap: MutableMap<String, ByteArray>
    fun insertSesame(sesame: CHDevices, result: CHResult<CHEmpty>)
    fun removeSesame(tag: String, result: CHResult<CHEmpty>)
}
```
`CHSesameConnector` 是一个设备连接器接口，继承自 `CHDevices` 接口。它负责管理和操作 Sesame 设备。

## 属性

- `ssm2KeysMap: MutableMap<String, ByteArray>` - 存储 Sesame2 设备的密钥映射。

## 方法

- `fun insertSesame(sesame: CHDevices, result: CHResult<CHEmpty>)` - 插入一个 Sesame 设备，并返回操作结果。
- `fun removeSesame(tag: String, result: CHResult<CHEmpty>)` - 根据给定的标签移除一个 Sesame 设备，并返回操作结果。

