# CHBaseDevice 类文档

```svg
@SuppressLint("MissingPermission") internal open class CHBaseDevice {
    lateinit var productModel: CHProductModel
    val gattRxBuffer: SesameBleReceiver = SesameBleReceiver() //[數據層][收]
    var gattTxBuffer: SesameBleTransmit? = null //[數據層][傳]
    lateinit var mSesameToken: ByteArray//第一次連線的時候從設備收亂數token準備驗證
    var mCharacteristic: BluetoothGattCharacteristic? = null //用來發送資料給
    var delegate: CHDeviceStatusDelegate? = null
    var deviceTimestamp:Long? = null
    var loginTimestamp:Long? = null
    var deviceId: UUID? = null
    var isRegistered: Boolean = true
    var rssi: Int? = 0
    var mBluetoothGatt: BluetoothGatt? = null 
    var isNeedAuthFromServer: Boolean? = false
    var mechStatus: CHSesameProtocolMechStatus? = null
    var deviceShadowStatus: CHDeviceStatus? = null
    var deviceStatus: CHDeviceStatus = CHDeviceStatus.NoBleSignal
    var sesame2KeyData: CHDevice? = null
    fun dropKey(result: CHResult<CHEmpty>)
    fun disconnect(result: CHResult<CHEmpty>)
        
}

```



`CHBaseDevice` 是一个开放的内部类。

## 属性

- `productModel`: 类型为 `CHProductModel`，产品模型。
- `gattRxBuffer`: 类型为 `SesameBleReceiver`，蓝牙接收缓冲区。
- `gattTxBuffer`: 类型为 `SesameBleTransmit?`，蓝牙传输缓冲区。
- `mSesameToken`: 类型为 `ByteArray`，设备的Token。
- `mCharacteristic`: 类型为 `BluetoothGattCharacteristic?`，用于发送数据。
- `delegate`: 类型为 `CHDeviceStatusDelegate?`，设备状态代理。
- `deviceTimestamp`: 类型为 `Long?`，设备时间戳。
- `loginTimestamp`: 类型为 `Long?`，登录时间戳。
- `deviceId`: 类型为 `UUID?`，设备ID。
- `isRegistered`: 类型为 `Boolean`，是否注册。
- `rssi`: 类型为 `Int?`，接收信号强度指示符。
- `mBluetoothGatt`: 类型为 `BluetoothGatt?`，蓝牙Gatt。
- `isNeedAuthFromServer`: 类型为 `Boolean?`，是否需要从服务器获取认证。
- `mechStatus`: 类型为 `CHSesameProtocolMechStatus?`，机械状态。
- `deviceShadowStatus`: 类型为 `CHDeviceStatus?`，设备阴影状态。
- `deviceStatus`: 类型为 `CHDeviceStatus`，设备状态。
- `sesame2KeyData`: 类型为 `CHDevice?`，设备密钥数据。

## 方法

1. `dropKey(result: CHResult<CHEmpty>)`: 删除设备的密钥
2. `disconnect(result: CHResult<CHEmpty>)`: 断开设备的连接


