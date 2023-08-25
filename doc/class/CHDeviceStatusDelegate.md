

# CHDeviceStatusDelegate 接口
```svg
interface CHDeviceStatusDelegate {

    fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {}
  
    fun onMechStatus(device: CHDevices) {}
}

```

`CHDeviceStatusDelegate` 是一个设备状态代理接口，用于监控和响应设备状态的变化。

## 方法

- `fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {}` - 当 BLE 设备状态发生变化时被调用，传入参数为设备对象、新的设备状态和设备的阴影状态（如果有）。
- `fun onMechStatus(device: CHDevices) {}` - 当设备的机械状态发生变化时被调用，传入参数为设备对象。


