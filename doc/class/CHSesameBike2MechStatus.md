# CHSesameBike2MechStatus 类

```svg
class CHSesameBike2MechStatus(override val data: ByteArray) : CHSesameProtocolMechStatus {
    private val battery = bytesToShort(data[0], data[1])
    private val flags = data[2].toInt()
    override var isInLockRange: Boolean = flags and 2 > 0
    override var isStop: Boolean? = flags and 4 > 0
    override fun getBatteryVoltage(): Float
}

```

`CHSesameBike2MechStatus` 类实现了 `CHSesameProtocolMechStatus` 接口，主要用于表示 Sesame Bike2 的机械状态。

## 属性

- `data: ByteArray` - 表示设备状态的字节数组。
- `isInLockRange: Boolean` - 表示设备是否在锁定范围内。
- `isStop: Boolean?` - 表示设备是否已经停止。

## 方法

- `fun getBatteryVoltage(): Float` - 获取设备的电池电压。

## 私有属性

- `battery: Short` - 表示设备的电池电量。
- `flags: Int` - 表示设备的标志位。

## 私有方法

- `bytesToShort(byte1: Byte, byte2: Byte): Short` - 将两个字节转换为一个短整数。

