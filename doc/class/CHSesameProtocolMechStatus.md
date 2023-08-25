# CHSesameProtocolMechStatus 接口
```svg


interface CHSesameProtocolMechStatus {

    val position: Short
       
    val target: Short?
      
    val isBatteryCritical: Boolean
        
    val isInLockRange: Boolean
   
    val isInUnlockRange: Boolean
   
    val isStop: Boolean?
     

    val data: ByteArray

    fun getBatteryVoltage(): Float

    fun getBatteryPrecentage(): Int 

}
```
`CHSesameProtocolMechStatus` 是一个设备机械状态协议接口，用于获取和处理设备的机械状态信息。

## 属性

- `position: Short` - 获取设备的位置，默认值为0。
- `target: Short?` - 获取设备的目标位置，默认值为0。
- `isBatteryCritical: Boolean` - 判断设备的电池是否处于临界状态，默认值为false。
- `isInLockRange: Boolean` - 判断设备是否在锁定范围内，默认值为false。
- `isInUnlockRange: Boolean` - 判断设备是否在解锁范围内，默认值为设备不在锁定范围内。
- `isStop: Boolean?` - 判断设备是否停止，默认值为null。
- `data: ByteArray` - 获取设备的数据。

## 方法

- `fun getBatteryVoltage(): Float` - 获取设备的电池电压。
- `fun getBatteryPrecentage(): Int` - 获取设备的电池电量百分比。该方法根据电池电压值计算电量百分比。


