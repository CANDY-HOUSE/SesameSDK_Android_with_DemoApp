

# CHDeviceStatusDelegate インターフェース
```svg
interface CHDeviceStatusDelegate {

    fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {}
  
    fun onMechStatus(device: CHDevices) {}
}

```

`CHDeviceStatusDelegate` はデバイス状態の変化を監視と応答するためのデバイス状態の代理のインターフェースです。

## メソッド

- `fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {}` - BLEデバイスの状態が変化した時に呼び出され、デバイスオブジェクト、新しいデバイスの状態、およびデバイスのシャドウ状態（存在する場合）というパラメータがインプットされます。
- `fun onMechStatus(device: CHDevices) {}` - デバイスの機械状態が変化した時に呼び出され、デバイスオブジェクトというパラメータがインプットされます。


