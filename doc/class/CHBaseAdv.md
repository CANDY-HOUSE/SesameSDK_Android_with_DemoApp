# CHBaseAdv 接口
```svg

internal interface CHBaseAdv {
    val rssi: Int?
    val isRegistered: Boolean
    val adv_tag_b1: Boolean
    val deviceID: UUID?
    var device: BluetoothDevice
    var deviceName: String?
    var productModel: CHProductModel?
    var isConnecable: Boolean?
}
```


`CHBaseAdv` 是一个内部接口。

## 属性

- `rssi`: 类型为 `Int?`，接收信号强度指示符。
- `isRegistered`: 类型为 `Boolean`，是否已注册。
- `adv_tag_b1`: 类型为 `Boolean`，广播标签。
- `deviceID`: 类型为 `UUID?`，设备ID。
- `device`: 类型为 `BluetoothDevice`，蓝牙设备。
- `deviceName`: 类型为 `String?`，设备名称。
- `productModel`: 类型为 `CHProductModel?`，产品模型。
- `isConnecable`: 类型为 `Boolean?`，是否可连接。
