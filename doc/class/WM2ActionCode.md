
# WM2ActionCode 列挙型
```svg
internal enum class WM2ActionCode(val value: UByte) {
CODE_NON(0U),
REGISTER_WM2(1U),
LOGIN_WM2(2U),
UPDATE_WIFI_SSID(3U),
UPDATE_WIFI_PASSWORD(4U),
CONNECT_WIFI(5U),
NETWORK_STATUS(6U),
DELETE_SESAME(7U),
ADD_SESAME(8U),
INITIAL(13U),
CCCD(14U),
SESAME_KEYS(16U),
RESET_WM2(18U),
SCAN_WIFI_SSID(19U),
OPEN_OTA_SERVER(126U),
VERSION_TAG(127U),
}

```
`WM2ActionCode`列挙型は一連の操作コードを定義します。

## メンバー

- `CODE_NON`：値が0で、操作なしです。
- `REGISTER_WM2`：値が1で、WM2を登録します。
- `LOGIN_WM2`：値が2で、WM2を登録します。
- `UPDATE_WIFI_SSID`：値が3で、Wifi SSIDを更新します。
- `UPDATE_WIFI_PASSWORD`：値が4で、Wifiのパスワードを更新します。
- `CONNECT_WIFI`：値が5で、Wifiに接続します。
- `NETWORK_STATUS`：値が6で、ネットワークの状態です。
- `DELETE_SESAME`：値が7で、Sesameを削除します。
- `ADD_SESAME`：値が8で、Sesameを添加します。
- `INITIAL`：値が13で、初期的な操作です。
- `CCCD`：値が14で、CCCDを操作します。
- `SESAME_KEYS`：値が16で、Sesameのキーです。
- `RESET_WM2`：値が18で、WM2をリセットします。
- `SCAN_WIFI_SSID`：値が19で、Wifi SSIDをスキャンします。
- `OPEN_OTA_SERVER`：値が126で、OTAのサーバーをオープンします。
- `VERSION_TAG`：値が127で、バージョンタグです。
