


# CHSesameBike2Device 类
```svg
internal class CHSesameBike2Device : CHSesameOS3(), CHSesameBike2, CHDeviceUtil {

    override var advertisement: CHadv? = null
     
    override fun unlock(tag: ByteArray?, result: CHResult<CHEmpty>) 
    override fun register(result: CHResult<CHEmpty>) 

    override fun login(token: String?) 

    override fun onGattSesamePublish(receivePayload: SSM3PublishPayload)
}

```

`CHSesameBike2Device`类继承了`CHSesameOS3`类，并实现了`CHSesameBike2`和`CHDeviceUtil`接口。它主要用于管理和控制自行车锁设备。

## 属性

- `advertisement`：设备广告数据。

## 方法

- `unlock(tag: ByteArray?, result: CHResult<CHEmpty>)`：解锁设备的函数。它接受一个字节数组标签和一个结果参数，并发送一个解锁命令到设备。
- `register(result: CHResult<CHEmpty>)`：注册设备的函数。它接受一个结果参数，并发送一个注册命令到设备。在注册期间，它还会处理设备状态和机械状态的更改。
- `login(token: String?)`：登录设备的函数。它接受一个令牌参数，并发送一个登录命令到设备。在登录期间，它还会处理设备状态的更改。
- `onGattSesamePublish(receivePayload: SSM3PublishPayload)`：处理GATT Sesame发布的函数。它接受一个发布负载参数，并更新设备和机械状态。


![CHSesameBike2Device](CHSesameBike2Device.svg)
