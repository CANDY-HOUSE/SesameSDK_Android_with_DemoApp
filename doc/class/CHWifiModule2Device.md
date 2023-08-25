

# CHWifiModule2Device
```svg
internal class CHWifiModule2Device : CHSesameOS3(), CHWifiModule2, CHDeviceUtil {

    // Properties
    override var ssm2KeysMap: MutableMap<String, String> = mutableMapOf()
    override var mechSetting: CHWifiModule2MechSettings? = CHWifiModule2MechSettings(null, null)
      


    override var advertisement: CHadv? = null
      

    override fun connect(result: CHResult<CHEmpty>) {
      
    }

    override fun register(result: CHResult<CHEmpty>) {
       
    }

    override fun login(token: String?) {
    }

    override fun scanWifiSSID(result: CHResult<CHEmpty>) {
    }

    override fun setWifiSSID(ssid: String, result: CHResult<CHEmpty>) {
    }

    override fun setWifiPassword(password: String, result: CHResult<CHEmpty>) {
    }

    override fun connectWifi(result: CHResult<CHEmpty>) {
    }

    override fun insertSesames(sesame: CHDevices, result: CHResult<CHEmpty>) {
    }

    override fun removeSesame(sesameKeyTag: String, result: CHResult<CHEmpty>) {
    }

    override fun getVersionTag(result: CHResult<String>) {
    }

    override fun reset(result: CHResult<CHEmpty>) {
    }

    override fun updateFirmware(onResponse: CHResult<BluetoothDevice>) {
    }

  
    val mBluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback()
  
    private fun onGattWM2Publish(receivePayload: SSM3PublishPayload) {
    }
}

```
`CHWifiModule2Device` 是一个内部类，继承自 `CHSesameOS3`，并实现了 `CHWifiModule2` 和 `CHDeviceUtil` 接口。它主要用于处理与Wi-Fi模块2设备相关的操作。

## 属性

- `ssm2KeysMap`：一个`MutableMap`，用于存储键值对。
- `mechSetting`：一个`CHWifiModule2MechSettings`对象，用于设置机械设定。
- `advertisement`：一个`CHadv`对象，用于广告。

## 方法

- `connect(result: CHResult<CHEmpty>)`：连接方法。
- `register(result: CHResult<CHEmpty>)`：注册方法。
- `login(token: String?)`：登录方法。
- `scanWifiSSID(result: CHResult<CHEmpty>)`：扫描Wi-Fi SSID方法。
- `setWifiSSID(ssid: String, result: CHResult<CHEmpty>)`：设置 Wi-Fi SSID方法。
- `setWifiPassword(password: String, result: CHResult<CHEmpty>)`：设置Wi-Fi密码方法。
- `connectWifi(result: CHResult<CHEmpty>)`：连接到Wi-Fi的方法。
- `insertSesames(sesame: CHDevices, result: CHResult<CHEmpty>)`：插入sesames方法。
- `removeSesame(sesameKeyTag: String, result: CHResult<CHEmpty>)`：删除sesame方法。
- `getVersionTag(result: CHResult<String>)`：获取版本标签的方法。
- `reset(result: CHResult<CHEmpty>)`：重置方法。
- `updateFirmware(onResponse: CHResult<BluetoothDevice>)`：更新固件的方法。
- `mBluetoothGattCallback`：一个`BluetoothGattCallback`对象，用于处理蓝牙GATT回调。
- `onGattWM2Publish(receivePayload: SSM3PublishPayload)`：处理 GATT WM2 发布的方法。


![CHWifiModule2Device](CHWifiModule2Device.svg)


