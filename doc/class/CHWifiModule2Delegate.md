
# CHWifiModule2Delegate 接口

```
interface CHWifiModule2Delegate : CHDeviceStatusDelegate {
fun onAPSettingChanged(device: CHWifiModule2, settings: CHWifiModule2MechSettings) {}
//    fun onNetWorkStatusChanged(device: CHWifiModule2, settings: CHWifiModule2NetWorkStatus) {}
fun onSSM2KeysChanged(device: CHWifiModule2, ssm2keys: Map<String, String>) {}
fun onOTAProgress(device: CHWifiModule2, percent: Byte) {}
fun onScanWifiSID(device: CHWifiModule2, ssid: String, rssi: Short) {}
}
```
`CHWifiModule2Delegate` 是一个设备接口，继承自 `CHDeviceStatusDelegate` 接口。这个接口用于管理和操作 CHWifiModule2 设备。

## 方法

- `fun onAPSettingChanged(device: CHWifiModule2, settings: CHWifiModule2MechSettings) {}` - 当 AP 设置变更时调用。

- `fun onNetWorkStatusChanged(device: CHWifiModule2, settings: CHWifiModule2NetWorkStatus) {}` - 当网络状态变更时调用（此方法已被注释掉）。

- `fun onSSM2KeysChanged(device: CHWifiModule2, ssm2keys: Map<String, String>) {}` - 当 SSM2Keys 变更时调用。

- `fun onOTAProgress(device: CHWifiModule2, percent: Byte) {}` - 当 OTA 进度变更时调用。

- `fun onScanWifiSID(device: CHWifiModule2, ssid: String, rssi: Short) {}` - 当扫描到 Wifi SID 时调用。

以上是 `CHWifiModule2Delegate` 接口的基本描述，这个接口为 CHWifiModule2 设备提供了一套完整的操作和管理方法。
