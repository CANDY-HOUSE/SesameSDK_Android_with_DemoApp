# CHBaseAdv インターフェース
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


`CHBaseAdv` は内部インターフェースです。

## プロパティ

- `rssi`: `Int?`型で、受信号強度の指示子です。
- `isRegistered`: `Boolean`型で、登録しているかどうかを判断します。
- `adv_tag_b1`: `Boolean`型で、アドバタイジングのタグです。
- `deviceID`: `UUID?`型で、デバイスIDです。
- `device`: `BluetoothDevice`型で，Bluetoothデバイスです。
- `deviceName`: `String?`型で、デバイス名です。 
- `productModel`: `CHProductModel?`型で、製品モデルです。
- `isConnecable`: `Boolean?`型で、接続可能かどうかを判断します。
