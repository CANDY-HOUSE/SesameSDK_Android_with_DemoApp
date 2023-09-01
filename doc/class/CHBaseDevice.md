# CHBaseDevice クラス

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



`CHBaseDevice`は公開された内部クラスです。

## プロパティ

- `productModel`: `CHProductModel`型で、製品モデルです。
- `gattRxBuffer`: `SesameBleReceiver`型で、Bluetooth受信バッファです。
- `gattTxBuffer`: `SesameBleTransmit?`型で、Bluetooth伝送バッファです。
- `mSesameToken`: `ByteArray`型で、デバイスのTokenです。
- `mCharacteristic`: `BluetoothGattCharacteristic?`型で、データ送信用です。
- `delegate`: `CHDeviceStatusDelegate?`型で、デバイス状態のDelegateです。
- `deviceTimestamp`: `Long?`型で、デバイスのタイムスタンプです。
- `loginTimestamp`: `Long?`型で、ログインのタイムスタンプです。
- `deviceId`: `UUID?`型で、デバイスIDです。
- `isRegistered`:  `Boolean`型で、登録しているかどうかを判断します。
- `rssi`: `Int?`型で、受信号強度の指示子です。
- `mBluetoothGatt`: `BluetoothGatt?`型で、Bluetooth  Gattです。
- `isNeedAuthFromServer`: `Boolean?`型で、サーバーから認証を取得する必要があるかを判断します。
- `mechStatus`: `CHSesameProtocolMechStatus?`型で、機械状態です。
- `deviceShadowStatus`: `CHDeviceStatus?`型で、デバイスのシャドウ状態です。
- `deviceStatus`: `CHDeviceStatus`型で、デバイスの状態です。
- `sesame2KeyData`: `CHDevice?`型で、デバイスのキーデータです。

##  メソッド

1. `dropKey(result: CHResult<CHEmpty>)`: デバイスのキーを削除します。
2. `disconnect(result: CHResult<CHEmpty>)`: デバイスの接続を切断します。


