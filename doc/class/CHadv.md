# CHadv 类
```svg
internal class CHadv(scanResult: ScanResult) : CHBaseAdv {


    override var isConnecable: Boolean? = true
    private val advBytes = scanResult.scanRecord?.manufacturerSpecificData?.valueAt(0)!!

    override var isRegistered: Boolean = (advBytes[2] and 1) > 0
    override var adv_tag_b1: Boolean = (advBytes[2] and 2) > 0
    override val rssi: Int = scanResult.rssi
    override var device = scanResult.device
    override var deviceName = scanResult.scanRecord?.deviceName
    override var productModel: CHProductModel? = CHProductModel.getByValue(advBytes.copyOfRange(0, 1).toBigLong().toInt())


    override val deviceID: UUID?
        
}

```


`CHadv` 是一个内部类，实现了 `CHBaseAdv` 接口。

## 属性

- `isConnecable`: 类型为 `Boolean?`，是否可连接，默认为 `true`。
- `isRegistered`: 类型为 `Boolean`，是否已注册，由 `advBytes[2] and 1` 决定。
- `adv_tag_b1`: 类型为 `Boolean`，广播标签，由 `advBytes[2] and 2` 决定。
- `rssi`: 类型为 `Int`，接收信号强度指示符，由 `scanResult.rssi` 决定。
- `device`: 类型为 `BluetoothDevice`，蓝牙设备，由 `scanResult.device` 决定。
- `deviceName`: 类型为 `String?`，设备名称，由 `scanResult.scanRecord?.deviceName` 决定。
- `productModel`: 类型为 `CHProductModel?`，产品模型，由 `CHProductModel.getByValue(advBytes.copyOfRange(0, 1).toBigLong().toInt())` 决定。
- `deviceID`: 类型为 `UUID?`，设备ID。

## 构造函数

- `CHadv(scanResult: ScanResult)`: 使用 `ScanResult` 对象创建 `CHadv` 实例。

## 私有属性

- `advBytes`: 类型为 `ByteArray`，广播字节，由 `scanResult.scanRecord?.manufacturerSpecificData?.valueAt(0)!!` 决定。
