# CHBaseDevice クラスのドキュメント
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
