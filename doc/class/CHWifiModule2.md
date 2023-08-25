# CHWifiModule2 接口
```svg
interface CHWifiModule2 : CHDevices {
    var ssm2KeysMap: MutableMap<String, String>
//    var networkStatus: CHWifiModule2NetWorkStatus?
    var mechSetting: CHWifiModule2MechSettings?
    fun scanWifiSSID(result: CHResult<CHEmpty>)
    fun setWifiSSID(ssid: String, result: CHResult<CHEmpty>)
    fun setWifiPassword(password: String, result: CHResult<CHEmpty>)
    fun connectWifi(result: CHResult<CHEmpty>)
    fun insertSesames(sesame: CHDevices, result: CHResult<CHEmpty>)
    fun removeSesame(sesameKeyTag: String, result: CHResult<CHEmpty>)
}

```


`CHWifiModule2` 是一个设备相关的接口，继承自 `CHDevices` 接口。除了 `CHDevices` 的基本操作外，还包含了对 WiFi 模块的一些操作。

## 属性

- `ssm2KeysMap: MutableMap<String, String>` - Sesame2密钥映射
- `mechSetting: CHWifiModule2MechSettings?` - WiFi模块的机械设置

## 方法

- `fun scanWifiSSID(result: CHResult<CHEmpty>)` - 扫描WiFi SSID
- `fun setWifiSSID(ssid: String, result: CHResult<CHEmpty>)` - 设置WiFi SSID
- `fun setWifiPassword(password: String, result: CHResult<CHEmpty>)` - 设置WiFi密码
- `fun connectWifi(result: CHResult<CHEmpty>)` - 连接到WiFi
- `fun insertSesames(sesame: CHDevices, result: CHResult<CHEmpty>)` - 插入Sesame设备
- `fun removeSesame(sesameKeyTag: String, result: CHResult<CHEmpty>)` - 移除Sesame设备


