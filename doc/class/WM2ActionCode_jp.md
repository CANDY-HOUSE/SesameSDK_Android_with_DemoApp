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
`WM2ActionCode` は、一連の操作コードを定義する列挙型です。

## メンバー

- `CODE_NON`：操作なし。値は0です。
- `REGISTER_WM2`：WM2を登録します。値は1です。
- `LOGIN_WM2`：WM2にログインします。値は2です。
- `UPDATE_WIFI_SSID`：WifiのSSIDを更新します。値は3です。
- `UPDATE_WIFI_PASSWORD`：Wifiのパスワードを更新します。値は4です。
- `CONNECT_WIFI`：Wifiに接続します。値は5です。
- `NETWORK_STATUS`：ネットワークの状態です。値は6です。
- `DELETE_SESAME`：Sesameを削除します。値は7です。
- `ADD_SESAME`：Sesameを追加します。値は8です。
- `INITIAL`：初期操作です。値は13です。
- `CCCD`：CCCD操作です。値は14です。
- `SESAME_KEYS`：Sesameの鍵情報です。値は16です。
- `RESET_WM2`：WM2をリセットします。値は18です。
- `SCAN_WIFI_SSID`：WifiのSSIDをスキャンします。値は19です。
- `OPEN_OTA_SERVER`：OTAサーバーを開きます。値は126です。
- `VERSION_TAG`：バージョンタグです。値は127です。
