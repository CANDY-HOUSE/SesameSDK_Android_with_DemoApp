
# CHSesameBotMechStatus 类
```svg
class CHSesameBotMechStatus(override val data: ByteArray) : CHSesameProtocolMechStatus {
    private val battery = bytesToShort(data[0], data[1])
    internal val motorStatus = data[4]
    private val flags = data[7].toInt()
    override var isInLockRange: Boolean = flags and 2 > 0
    override var isInUnlockRange: Boolean = flags and 4 > 0
    override var isBatteryCritical: Boolean = flags and 32 > 0
    override var isStop: Boolean? = (flags and 1 == 0)
    override fun getBatteryVoltage(): Float 
}


```
`CHSesameBotMechStatus`类主要用于处理和管理Sesame Bot的机械状态。

## 属性

- `data`：字节数组，ByteArray类型，存储有效载荷数据。
- `battery`：电池状态，通过字节转换为短整型获得。
- `motorStatus`：电机状态，从`data`数组的第5个元素获得。
- `flags`：标志，从`data`数组的第8个元素转换为整型获得。
- `isInLockRange`：是否在锁定范围内，Boolean类型，通过与2进行位与运算判断。
- `isInUnlockRange`：是否在解锁范围内，Boolean类型，通过与4进行位与运算判断。
- `isBatteryCritical`：电池是否严重，Boolean类型，通过与32进行位与运算判断。
- `isStop`：是否停止，Boolean类型，通过与1进行位与运算并判断结果是否为0来确定。

## 方法

- `getBatteryVoltage()`：此方法用于获取电池电压，返回Float类型的电压值。电压等于电池状态的值乘以7.2，再除以1023。

## 继承

- `CHSesameProtocolMechStatus`：`CHSesameBotMechStatus`类继承了`CHSesameProtocolMechStatus`类，需要实现父类中的`data`属性和`getBatteryVoltage()`方法。
