
# CHWifiModule2Delegate インターフェース

```
interface CHWifiModule2Delegate : CHDeviceStatusDelegate {
fun onAPSettingChanged(device: CHWifiModule2, settings: CHWifiModule2MechSettings) {}
//    fun onNetWorkStatusChanged(device: CHWifiModule2, settings: CHWifiModule2NetWorkStatus) {}
fun onSSM2KeysChanged(device: CHWifiModule2, ssm2keys: Map<String, String>) {}
fun onOTAProgress(device: CHWifiModule2, percent: Byte) {}
fun onScanWifiSID(device: CHWifiModule2, ssid: String, rssi: Short) {}
}
```
`CHWifiModule2Delegate` は`CHDeviceStatusDelegate` インターフェースを継承し、デバイスのインターフェースです。 CHWifiModule2 デバイスの管理と操作に特化しています。

## メソッド

- `fun onAPSettingChanged(device: CHWifiModule2, settings: CHWifiModule2MechSettings) {}` -  AP 設定が変更された時に呼び出されます。

- `fun onNetWorkStatusChanged(device: CHWifiModule2, settings: CHWifiModule2NetWorkStatus) {}` - ネットワークの状態が変更された時に呼び出されます（このメソッドはコメントアウトされています）。

- `fun onSSM2KeysChanged(device: CHWifiModule2, ssm2keys: Map<String, String>) {}` - SSM2Keysが変更された時に呼び出されます。

- `fun onOTAProgress(device: CHWifiModule2, percent: Byte) {}` - OTAの進捗状況が変更された時に呼び出されます。

- `fun onScanWifiSID(device: CHWifiModule2, ssid: String, rssi: Short) {}` - Wifi SID をスキャンした時に呼び出されます。

以上は `CHWifiModule2Delegate` インタフェースについての基本的な説明です。CHWifiModule2デバイスに対して完全な操作と管理方法を提供します。
