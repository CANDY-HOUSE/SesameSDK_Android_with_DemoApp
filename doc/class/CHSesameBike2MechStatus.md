# CHSesameBike2MechStatus クラス

```svg
class CHSesameBike2MechStatus(override val data: ByteArray) : CHSesameProtocolMechStatus {
    private val battery = bytesToShort(data[0], data[1])
    private val flags = data[2].toInt()
    override var isInLockRange: Boolean = flags and 2 > 0
    override var isStop: Boolean? = flags and 4 > 0
    override fun getBatteryVoltage(): Float
}

```

`CHSesameBike2MechStatus`クラスは、`CHSesameProtocolMechStatus`インターフェースを実装しており、主にesame Bike2の機械状態を表します。

## プロパティ

- `data: ByteArray` - デバイスの状態を表すバイト配列です。
- `isInLockRange: Boolean` - デバイスがロックの範囲内にあるかどうかを示す情報です。
- `isStop: Boolean?` - デバイスが停止しているかどうかを示す情報です。

## メソッド

- `fun getBatteryVoltage(): Float` - デバイスのバッテリー電圧を取得します。

## 非公開プロパティ

- `battery: Short` - デバイスのバッテリー残量を示します。
- `flags: Int` - デバイスのフラグを示します。

## 非公開メソッド

- `bytesToShort(byte1: Byte, byte2: Byte): Short` - 2つのバイトをショート整数に変換します。

