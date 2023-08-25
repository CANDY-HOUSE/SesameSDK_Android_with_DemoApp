# CHBaseAdv インターフェースのドキュメント
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
`CHBaseAdv` は、内部インターフェースです。

## プロパティ

- `rssi`：`Int?` 型のプロパティで、受信信号強度指示子（RSSI）を受け取ります。
- `isRegistered`：`Boolean` 型のプロパティで、登録済みかどうかを示します。
- `adv_tag_b1`：`Boolean` 型のプロパティで、広告タグを表します。
- `deviceID`：`UUID?` 型のプロパティで、デバイスのUUIDを示します。
- `device`：`BluetoothDevice` 型のプロパティで、Bluetoothデバイスを表します。
- `deviceName`：`String?` 型のプロパティで、デバイス名を示します。
- `productModel`：`CHProductModel?` 型のプロパティで、製品モデルを表します。
- `isConnecable`：`Boolean?` 型のプロパティで、接続可能かどうかを示します。
