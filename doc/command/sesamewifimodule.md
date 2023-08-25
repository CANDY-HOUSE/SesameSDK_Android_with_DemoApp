#   Wifi Module 2 讲解
## 实现类 CHWifiModule2Device
### 接口

```agsl
    fun scanWifiSSID(result: CHResult<CHEmpty>)
    fun setWifiSSID(ssid: String, result: CHResult<CHEmpty>)
    fun setWifiPassword(password: String, result: CHResult<CHEmpty>)
    fun connectWifi(result: CHResult<CHEmpty>)
    fun insertSesames(sesame: CHDevices, result: CHResult<CHEmpty>)
    fun removeSesame(sesameKeyTag: String, result: CHResult<CHEmpty>)
    fun reset(result: CHResult<CHEmpty>)
    fun getVersionTag(result: CHResult<CHEmpty>)
```
### 接口功能字义
- [scanWifiSSID](../wm2/scanwifissid.md) :扫描wifi,获取SSID
- [setWifiSSID](../wm2/setwifissid.md):设置SSID
- [setWifiPassword](../wm2/setwifipw.md):设置WIFI密码
- [connectWifi](../wm2/connectwifi.md):连接Wifi
- [insertSesames](../touch/add_sesame.md):添加Sesames系列产品
- [removeSesame](../touch/remove_sesame.md):移除Sesames系列产品
- [getVersionTag](ssm5version.md):获取版本号
- [reset](reset.md):重置设备
### 循环图
![CHWifiModule2Device](../class/CHWifiModule2Device.svg)






