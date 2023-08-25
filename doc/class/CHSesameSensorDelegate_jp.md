# CHSesameSensorDelegate インターフェースのドキュメント
```svg
interface CHSesameSensorDelegate : CHDeviceStatusDelegate {
fun onSSM2KeysChanged(device: CHSesameConnector, ssm2keys: Map<String, ByteArray>) {}
}

```

`CHSesameSensorDelegate` インターフェースは、Sesame デバイスのセンサー関連イベントを処理および管理するために使用されます。

## メソッド

- `onSSM2KeysChanged(device: CHSesameConnector, ssm2keys: Map<String, ByteArray>)`：このメソッドは、SSM2 キーが変更された際に呼び出されます。このメソッドは 2 つのパラメータを受け取ります。第 1 パラメータはキーが変更されたデバイス、第 2 パラメータは新しい SSM2 キーのマップです。デフォルトの実装では何も行いません。

## 継承

- `CHDeviceStatusDelegate`：`CHSesameSensorDelegate` インターフェースは、`CHDeviceStatusDelegate` インターフェースを継承しており、親クラスのすべてのメソッドを実装する必要があります。
