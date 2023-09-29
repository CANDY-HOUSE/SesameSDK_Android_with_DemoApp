#   Wifi Module 2 説明
## 実装クラス CHWifiModule2Device
### インターフェース

```agsl
    fun scanWifiSSID(result: CHResult<CHEmpty>)
    fun setWifiSSID(ssid: String, result: CHResult<CHEmpty>)
    fun setWifiPassword(password: String, result: CHResult<CHEmpty>)
    fun connectWifi(result: CHResult<CHEmpty>)
    fun insertSesames(sesame: CHDevices, result: CHResult<CHEmpty>)
    fun removeSesame(sesameKeyTag: String, result: CHResult<CHEmpty>)
    fun reset(result: CHResult<CHEmpty>)
    fun getVersionTag(result: CHResult<CHEmpty>)
```
### インターフェースの機能の定義
- [scanWifiSSID](../wm2/scanwifissid.md) :wifiをスキャンし、SSIDを取得する
- [setWifiSSID](../wm2/setwifissid.md):SSIDを設置する
- [setWifiPassword](../wm2/setwifipw.md):WIFIパスワードを設置する
- [connectWifi](../wm2/connectwifi.md):Wifiに接続する
- [insertSesames](../touch/add_sesame.md):Sesamesシリーズ製品を追加する
- [removeSesame](../touch/remove_sesame.md):移除Sesamesシリーズ製品を削除する
- [getVersionTag](ssm5version.md):バージョンのタグを取得する
- [reset](reset.md):デバイスをリセットする
### フローチャート
![CHWifiModule2Device](../class/CHWifiModule2Device.svg)






