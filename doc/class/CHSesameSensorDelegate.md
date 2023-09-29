
# CHSesameSensorDelegate インターフェース
```svg
interface CHSesameSensorDelegate : CHDeviceStatusDelegate {
fun onSSM2KeysChanged(device: CHSesameConnector, ssm2keys: Map<String, ByteArray>) {}
}

```
`CHSesameSensorDelegate`インターフェースは、Sesameデバイスのセンサー関連イベントを処理および管理します。

## メソッド

- `onSSM2KeysChanged(device: CHSesameConnector, ssm2keys: Map<String, ByteArray>)`：SSM2キーが変更された際に呼び出されます。このメソッドは2つのパラメータを受け取ります。1つ目のパラメータはキーの変更が発生したデバイスであり、2つ目のパラメータは新しいSSM2のキーセットです。デフォルト値は null です。

## 継承

- `CHDeviceStatusDelegate`：`CHSesameSensorDelegate`インターフェースは`CHDeviceStatusDelegate`を継承し、親クラスの全てのメソッドを実装します。
