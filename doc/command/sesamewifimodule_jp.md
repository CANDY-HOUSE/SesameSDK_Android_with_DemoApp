#   Wifi Module 2 解説
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
### インターフェースの機能説明

- [scanWifiSSID](../wm2/scanwifissid_jp.md) :Wi-Fiをスキャンし、SSIDを取得します
- [setWifiSSID](../wm2/setwifissid_jp.md):Wi-Fiをスキャンし、SSIDを取得します
- [setWifiPassword](../wm2/setwifipw_jp.md):Wi-Fiのパスワードを設定します
- [connectWifi](../wm2/connectwifi_jp.md):Wi-Fiに接続します
- [insertSesames](../touch/add_sesame_jp.md):Sesamesシリーズ製品を追加します
- [removeSesame](../touch/remove_sesame_jp.md):Sesamesシリーズ製品を削除します
- [getVersionTag](ssm5version_jp.md):バージョンタグを取得します
- [reset](reset_jp.md):デバイスをリセットします

### フローチャート

![CHWifiModule2Device](../class/CHWifiModule2Device.svg)






