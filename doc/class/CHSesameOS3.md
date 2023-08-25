

# CHSesameOS3 类
```svg

internal open class CHSesameOS3 : CHBaseDevice(), CHSesameOS3Publish {
    var cipher: SesameOS3BleCipher? = null

    var cmdCallBack: MutableMap<UByte, SesameOS3ResponseCallback> = mutableMapOf()

    var semaphore: Semaphore = Semaphore(1)

    open fun connect(result: CHResult<CHEmpty>) 

    private val mBluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() 

    fun transmit() 

    fun sendCommand(payload: SesameOS3Payload, isEncryt: DeviceSegmentType = DeviceSegmentType.cipher, onResponse: SesameOS3ResponseCallback) 

    open fun getVersionTag(result: CHResult<String>)

    open fun reset(result: CHResult<CHEmpty>)

    open fun updateFirmware(onResponse: CHResult<BluetoothDevice>)

    fun parceADV(value: CHadv?)

    override fun onGattSesamePublish(receivePayload: SSM3PublishPayload)
}

```
`CHSesameOS3` 是一个开放的内部类，继承自 `CHBaseDevice`，并实现了 `CHSesameOS3Publish` 接口。

## 成员变量

- `var cipher: SesameOS3BleCipher?`: 用于加解密的实例。

- `var cmdCallBack: MutableMap<UByte, SesameOS3ResponseCallback>`: 用于存储回调函数的映射。

- `var semaphore: Semaphore`: 用于控制并发的信号量。

## 方法

- `open fun connect(result: CHResult<CHEmpty>)`: 连接设备的方法。

- `private val mBluetoothGattCallback: BluetoothGattCallback`: Bluetooth GATT 回调函数。

- `fun transmit()`: 传输数据。

- `fun sendCommand(payload: SesameOS3Payload, isEncryt: DeviceSegmentType = DeviceSegmentType.cipher, onResponse: SesameOS3ResponseCallback)`: 发送命令。

- `open fun getVersionTag(result: CHResult<String>)`: 获取版本标签。

- `open fun reset(result: CHResult<CHEmpty>)`: 重置设备。

- `open fun updateFirmware(onResponse: CHResult<BluetoothDevice>)`: 更新固件。

- `fun parceADV(value: CHadv?)`: 解析广播数据。

- `override fun onGattSesamePublish(receivePayload: SSM3PublishPayload)`: 处理 GATT 发布消息。

以上是 `CHSesameOS3` 类的基本描述，这个类为 Sesame OS3 设备提供了一套完整的操作和管理方法。
