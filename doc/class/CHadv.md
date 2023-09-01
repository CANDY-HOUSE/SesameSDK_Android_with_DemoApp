# CHadv クラス
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


`CHadv`は、`CHBaseAdv`インターフェースを実現できた内部クラスです。

## プロパティ

- `isConnecable`: `Boolean?`型で、接続可能かどうかを判断します。デフォルトは `true` です。
- `isRegistered`: `Boolean`型で、登録しているかどうかを判断します。`advBytes[2] and 1` によって決まります。
- `adv_tag_b1`: `Boolean`型で、アドバタイジングのタグです。`advBytes[2] and 2` によって決まります。
- `rssi`: `Int`型で、受信号強度の指示子です。`scanResult.rssi` によって決まります。
- `device`: `BluetoothDevice`型で，Bluetoothデバイスです。 `scanResult.device` によって決まります。
- `deviceName`: `String?`型で、デバイス名です。 `scanResult.scanRecord?.deviceName` によって決まります。
- `productModel`: `CHProductModel?`型で、製品モデルです。 `CHProductModel.getByValue(advBytes.copyOfRange(0, 1).toBigLong().toInt())` によって決まります。
- `deviceID`:  `UUID?`型で、デバイスIDです。

## コンストラクター

- `CHadv(scanResult: ScanResult)`: `ScanResult` オブジェクトを使用して`CHadv`のインスタンスを作成します。 

## 非公開プロパティ

- `advBytes`: `ByteArray`型で、アドバタイジングのバイトです。`scanResult.scanRecord?.manufacturerSpecificData?.valueAt(0)!!` によって決まります。