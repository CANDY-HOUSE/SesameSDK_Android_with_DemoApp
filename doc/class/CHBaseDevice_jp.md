# CHBaseDevice クラスのドキュメント
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
`CHBaseDevice` は、公開された内部クラスです。

## プロパティ

- `productModel`：`CHProductModel` 型の変数で、製品モデルです。
- `gattRxBuffer`：`SesameBleReceiver` 型の変数で、Bluetoothの受信バッファです。
- `gattTxBuffer`：`SesameBleTransmit?` 型の変数で、Bluetoothの送信バッファです。
- `mSesameToken`：`ByteArray` 型の変数で、デバイスのトークンです。
- `mCharacteristic`：`BluetoothGattCharacteristic?` 型の変数で、データ送信に使用されます。
- `delegate`：`CHDeviceStatusDelegate?` 型の変数で、デバイスの状態を代理するものです。
- `deviceTimestamp`：`Long?` 型の変数で、デバイスのタイムスタンプです。
- `loginTimestamp`：`Long?` 型の変数で、ログインのタイムスタンプです。
- `deviceId`：`UUID?` 型の変数で、デバイスIDです。
- `isRegistered`：`Boolean` 型の変数で、登録済みかどうかを示します。
- `rssi`：`Int?` 型の変数で、受信信号強度指示子（RSSI）です。
- `mBluetoothGatt`：`BluetoothGatt?` 型の変数で、BluetoothGattです。
- `isNeedAuthFromServer`：`Boolean?` 型の変数で、サーバーからの認証が必要かどうかを示します。
- `mechStatus`：`CHSesameProtocolMechStatus?` 型の変数で、機械の状態です。
- `deviceShadowStatus`：`CHDeviceStatus?` 型の変数で、デバイスの影響状態です。
- `deviceStatus`：`CHDeviceStatus` 型の変数で、デバイスの状態です。
- `sesame2KeyData`：`CHDevice?` 型の変数で、デバイスの鍵データです。

## メソッド

1. `dropKey(result: CHResult<CHEmpty>)`：デバイスの鍵を削除します。
2. `disconnect(result: CHResult<CHEmpty>)`：デバイスの接続を切断します。
