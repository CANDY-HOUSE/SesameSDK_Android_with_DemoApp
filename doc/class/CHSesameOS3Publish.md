# CHSesameOS3Publish 接口

```svg
interface CHSesameOS3Publish {
    fun onGattSesamePublish(receivePayload: SSM3PublishPayload)
}

```



`CHSesameOS3Publish` 是一个接口。

## 方法

- `onGattSesamePublish(receivePayload: SSM3PublishPayload)`：这是一个需要被实现的方法，此方法接受一个 `SSM3PublishPayload` 类型的参数 `receivePayload`，用于处理接收到的发布负载。

