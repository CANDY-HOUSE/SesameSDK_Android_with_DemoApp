# CHSesameBike2MechStatus クラスのドキュメント
```svg
class CHSesameBike2MechStatus(override val data: ByteArray) : CHSesameProtocolMechStatus {
    private val battery = bytesToShort(data[0], data[1])
    private val flags = data[2].toInt()
    override var isInLockRange: Boolean = flags and 2 > 0
    override var isStop: Boolean? = flags and 4 > 0
    override fun getBatteryVoltage(): Float
}

```
`CHSesameBike2MechStatus` クラスは、`CHSesameProtocolMechStatus` インターフェースを実装したもので、主に Sesame Bike2 の機械の状態を表します。

## プロパティ

- `data: ByteArray` - デバイスの状態を表すバイト配列です。
- `isInLockRange: Boolean` - デバイスがロック範囲内にあるかどうかを示します。
- `isStop: Boolean?` - デバイスが停止しているかどうかを示します。

## メソッド

- `fun getBatteryVoltage(): Float` - デバイスのバッテリー電圧を取得します。

## プライベートプロパティ

- `battery: Short` - デバイスのバッテリー残量を示します。
- `flags: Int` - デバイスのフラグを示します。

## プライベートメソッド

- `bytesToShort(byte1: Byte, byte2: Byte): Short` - 二つのバイトを短整数に変換する関数です。
