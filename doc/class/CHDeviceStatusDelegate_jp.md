# CHDeviceStatusDelegate インターフェースのドキュメント
```svg
interface CHDeviceStatusDelegate {

    fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {}
  
    fun onMechStatus(device: CHDevices) {}
}

```

`CHDeviceStatusDelegate` は、デバイスの状態変更を監視し、それに応答するためのデバイス状態代理インターフェースです。

## メソッド

- `fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {}` - BLEデバイスの状態が変更された際に呼び出されます。デバイスオブジェクト、新しいデバイスの状態、およびデバイスのシャドウ状態（存在する場合）が引数として渡されます。
- `fun onMechStatus(device: CHDevices) {}` - デバイスのメカニカル状態が変更された際に呼び出されます。デバイスオブジェクトが引数として渡されます。
