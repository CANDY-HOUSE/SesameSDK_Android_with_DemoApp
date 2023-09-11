# CHWifiModule2 インターフェイス
```svg
interface CHWifiModule2 : CHDevices {
    var ssm2KeysMap: MutableMap<String, String>
//    var networkStatus: CHWifiModule2NetWorkStatus?
    var mechSetting: CHWifiModule2MechSettings?
    fun scanWifiSSID(result: CHResult<CHEmpty>)
    fun setWifiSSID(ssid: String, result: CHResult<CHEmpty>)
    fun setWifiPassword(password: String, result: CHResult<CHEmpty>)
    fun connectWifi(result: CHResult<CHEmpty>)
    fun insertSesames(sesame: CHDevices, result: CHResult<CHEmpty>)
    fun removeSesame(sesameKeyTag: String, result: CHResult<CHEmpty>)
}

```


`CHWifiModule2`は、デバイスに関連するインターフェースであり、`CHDevices`インターフェースを継承しています。`CHDevices`の基本的な操作に加えて、WiFiモジュールに関連する操作も含まれています。

## プロパティ

- `ssm2KeysMap: MutableMap<String, String>` - Sesame2のキーマップ
- `mechSetting: CHWifiModule2MechSettings?` - WiFiモジュールの機械設置

## メソッド

- `fun scanWifiSSID(result: CHResult<CHEmpty>)` - WiFi SSIDをスキャンします。
- `fun setWifiSSID(ssid: String, result: CHResult<CHEmpty>)` - WiFi SSIDを設置します。
- `fun setWifiPassword(password: String, result: CHResult<CHEmpty>)` - WiFiのパスワードを設置します。
- `fun connectWifi(result: CHResult<CHEmpty>)` - WiFiに接続します。
- `fun insertSesames(sesame: CHDevices, result: CHResult<CHEmpty>)` - Sesameデバイスを挿入します。
- `fun removeSesame(sesameKeyTag: String, result: CHResult<CHEmpty>)` - Sesameデバイスを削除します。


