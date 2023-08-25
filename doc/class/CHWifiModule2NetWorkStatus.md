
# CHWifiModule2NetWorkStatus 类
```svg
class CHWifiModule2NetWorkStatus(
    var isAPWork: Boolean?,
    var isNetWork: Boolean?,
    var isIOTWork: Boolean?,
    var isAPConnecting: Boolean,
    var isConnectingNet: Boolean,
    var isConnectingIOT: Boolean,
    var isAPCheck: Boolean?
) : CHSesameProtocolMechStatus {
    override val data: ByteArray
    override fun getBatteryVoltage(): Float 
}


```
`CHWifiModule2NetWorkStatus`类主要用于处理和管理Wifi module 2的网络状态。

## 属性

- `isAPWork`：AP是否工作，Boolean类型。
- `isNetWork`：是否有网络，Boolean类型。
- `isIOTWork`：IOT是否工作，Boolean类型。
- `isAPConnecting`：是否正在连接AP，Boolean类型。
- `isConnectingNet`：是否正在连接网络，Boolean类型。
- `isConnectingIOT`：是否正在连接IOT，Boolean类型。
- `isAPCheck`：是否检查AP，Boolean类型。

## 方法

- `getBatteryVoltage()`：此方法用于获取电池电压，返回Float类型的电压值。具体实现还未完成，需要后续进行实现。

## 继承

- `CHSesameProtocolMechStatus`：`CHWifiModule2NetWorkStatus`类继承了`CHSesameProtocolMechStatus`类，需要实现父类中的`data`属性和`getBatteryVoltage()`方法。
