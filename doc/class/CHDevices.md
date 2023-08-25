# CHDevices 接口

```svg

interface CHDevices {

    var mechStatus: CHSesameProtocolMechStatus?

    var deviceTimestamp:Long?

    var loginTimestamp:Long?

    var delegate: CHDeviceStatusDelegate?

    var deviceStatus: CHDeviceStatus

    var deviceShadowStatus: CHDeviceStatus?

    var rssi: Int?

    var deviceId: UUID?

    var isRegistered: Boolean

    var productModel: CHProductModel

    fun connect(result: CHResult<CHEmpty>)

    fun disconnect(result: CHResult<CHEmpty>)

    fun getKey(): CHDevice 
    fun dropKey(result: CHResult<CHEmpty>)

    fun getVersionTag(result: CHResult<String>)
                    
    fun register(result: CHResult<CHEmpty>)
                        
    fun reset(result: CHResult<CHEmpty>)
                            
    fun updateFirmware(onResponse: CHResult<BluetoothDevice>)

    fun createGuestKey(keyName: String, result: CHResult<CHDevice>) 

    fun getGuestKeys(result: CHResult<Array<CHGuestKeyCut>>) 

    fun removeGuestKey(guestKeyId: String, result: CHResult<CHEmpty>)

    fun updateGuestKey(guestKeyId: String, name: String, result: CHResult<CHEmpty>) 

    fun setHistoryTag(tag: ByteArray, result: CHResult<CHEmpty>) 

    fun getHistoryTag(): ByteArray? 

    fun getTimeSignature(): String 

}
```


`CHDevices` 是一个设备相关的接口，包含了对设备的一些基本操作，如连接、断开连接、获取版本、注册、重置、更新固件等。

## 属性

- `mechStatus: CHSesameProtocolMechStatus?` - 设备的机械状态
- `deviceTimestamp: Long?` - 设备的时间戳
- `loginTimestamp: Long?` - 登录的时间戳
- `delegate: CHDeviceStatusDelegate?` - 设备状态的代理
- `deviceStatus: CHDeviceStatus` - 设备的状态
- `deviceShadowStatus: CHDeviceStatus?` - 设备的阴影状态
- `rssi: Int?` - 接收信号强度指标
- `deviceId: UUID?` - 设备的唯一标识符
- `isRegistered: Boolean` - 设备是否已注册
- `productModel: CHProductModel` - 设备的产品模型

## 方法

- `fun connect(result: CHResult<CHEmpty>)` - 连接设备
- `fun disconnect(result: CHResult<CHEmpty>)` - 断开设备连接
- `fun getKey(): CHDevice` - 获取设备的密钥
- `fun dropKey(result: CHResult<CHEmpty>)` - 丢弃设备的密钥
- `fun getVersionTag(result: CHResult<String>)` - 获取设备的版本标签
- `fun register(result: CHResult<CHEmpty>)` - 注册设备
- `fun reset(result: CHResult<CHEmpty>)` - 重置设备
- `fun updateFirmware(onResponse: CHResult<BluetoothDevice>)` - 更新设备的固件
- `fun createGuestKey(keyName: String, result: CHResult<CHDevice>)` - 创建访客密钥
- `fun getGuestKeys(result: CHResult<Array<CHGuestKeyCut>>)` - 获取访客密钥
- `fun removeGuestKey(guestKeyId: String, result: CHResult<CHEmpty>)` - 移除访客密钥
- `fun updateGuestKey(guestKeyId: String, name: String, result: CHResult<CHEmpty>)` - 更新访客密钥
- `fun setHistoryTag(tag: ByteArray, result: CHResult<CHEmpty>)` - 设置历史标签
- `fun getHistoryTag(): ByteArray?` - 获取历史标签
- `fun getTimeSignature(): String` - 获取时间签名

