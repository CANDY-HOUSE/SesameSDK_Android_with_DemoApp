# CHSesameOS3Publish インターフェースのドキュメント
```svg
interface CHSesameOS3Publish {
    fun onGattSesamePublish(receivePayload: SSM3PublishPayload)
}

```
`CHSesameOS3Publish` は、インターフェースです。

## メソッド

- `onGattSesamePublish(receivePayload: SSM3PublishPayload)`：このメソッドは実装が必要なメソッドで、`SSM3PublishPayload` 型の引数 `receivePayload` を受け取り、受信したパブリッシュのペイロードを処理します。
