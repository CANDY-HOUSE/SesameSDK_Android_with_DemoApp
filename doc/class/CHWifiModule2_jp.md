# CHWifiModule2 インターフェースのドキュメント

`CHWifiModule2` は、デバイスに関連する操作を含むインターフェースであり、`CHDevices` インターフェースを継承しています。基本的なデバイス操作に加えて、WiFiモジュールに関連する操作も含まれています。

## 属性

- `ssm2KeysMap: MutableMap<String, String>` - Sesame2キーマップ
- `mechSetting: CHWifiModule2MechSettings?` - WiFiモジュールのメカニズム設定

## メソッド

- `fun scanWifiSSID(result: CHResult<CHEmpty>)` - WiFi SSIDをスキャンする
- `fun setWifiSSID(ssid: String, result: CHResult<CHEmpty>)` - WiFi SSIDを設定する
- `fun setWifiPassword(password: String, result: CHResult<CHEmpty>)` - WiFiパスワードを設定する
- `fun connectWifi(result: CHResult<CHEmpty>)` - WiFiに接続する
- `fun insertSesames(sesame: CHDevices, result: CHResult<CHEmpty>)` - Sesameデバイスを挿入する
- `fun removeSesame(sesameKeyTag: String, result: CHResult<CHEmpty>)` - Sesameデバイスを削除する
