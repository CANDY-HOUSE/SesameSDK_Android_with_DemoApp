# CHSesameOS3Publish インターフェース

```svg
interface CHSesameOS3Publish {
    fun onGattSesamePublish(receivePayload: SSM3PublishPayload)
}

```



`CHSesameOS3Publish` はインターフェースです。

## メソッド

- `onGattSesamePublish(receivePayload: SSM3PublishPayload)`：このメソッドは実装する必要があります。`SSM3PublishPayload`型の引数`receivePayload`を受取り、受信したPublishPayloadを処理します。

