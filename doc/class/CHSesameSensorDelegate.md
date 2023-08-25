
# CHSesameSensorDelegate 接口
```svg
interface CHSesameSensorDelegate : CHDeviceStatusDelegate {
fun onSSM2KeysChanged(device: CHSesameConnector, ssm2keys: Map<String, ByteArray>) {}
}

```
`CHSesameSensorDelegate`接口主要用于处理和管理Sesame设备的传感器相关事件。

## 方法

- `onSSM2KeysChanged(device: CHSesameConnector, ssm2keys: Map<String, ByteArray>)`：当SSM2密钥发生变化时调用此方法。此方法接收两个参数，第一个参数是发生密钥变化的设备，第二个参数是新的SSM2密钥集合。默认实现为空。

## 继承

- `CHDeviceStatusDelegate`：`CHSesameSensorDelegate`接口继承了`CHDeviceStatusDelegate`接口，需要实现父类中的所有方法。
