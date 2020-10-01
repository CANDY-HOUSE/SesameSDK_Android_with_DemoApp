# ドキュメント / Documentation / 文檔
[https://github.com/CANDY-HOUSE/SesameSDK_iOS_with_DemoApp](https://github.com/CANDY-HOUSE/SesameSDK_iOS_with_DemoApp)


# Beta WiFi機能

```kotlin

public interface CHWifiModule2 {
    var deviceId: UUID?       // WiFi Module2 の デバイスUUID
    var rssi: Int?            // スマホが拾った WiFi Module2 デバイス の Bluetooth RSSI
    var txPowerLevel: Int?    // WiFi Module2 デバイス が発信している Bluetooth TxPower
    var isRegistered: Boolean // WiFi Module2 デバイス が登録されているか否か?
    var deviceStatus: CHWifiModule2Status
    var delegate: CHWifiModule2Delegate?
    var networkStatus: CHWifiModule2NetWorkStatus? // WiFi Module2が    WiFi環境に接続できたか？ インターネットに接続できたか? CANDY HOUSEサーバーに接続できたか？
    var mechSetting: CHWifiModule2MechSettings?    // WiFi Module2には   WiFi環境のSSIDやパスワードが設定完了か否か?

    fun connect(result: CHResult<CHEmpty>)    // スマホが WiFi Module2 とBluetooth接続 をする
    fun disconnect(result: CHResult<CHEmpty>) // スマホが WiFi Module2 とBluetooth接続 を切る
    fun setWifiSSID(ssid: String, result: CHResult<CHEmpty>)         // スマホがWiFi Module2に WiFi環境のSSIDを設定する
    fun setWifiPassword(password: String, result: CHResult<CHEmpty>) // スマホがWiFi Module2に WiFi環境のパスワードを設定する
    fun connectWifi(result: CHResult<CHEmpty>)                       // WiFi Module2 がWiFi環境に接続する
    fun insertSesames(sesame: CHSesame2, result: CHResult<CHEmpty>)  // スマホがWiFi Module2デバイスに セサミデバイスを情報を伝達する

    fun testIOTLOCK( result: CHResult<CHEmpty>)   // スマホ --> CANDY HOUSEサーバー --> WiFiModule2 --> セサミを施錠する
    fun testIOTUNLOCK( result: CHResult<CHEmpty>) // スマホ --> CANDY HOUSEサーバー --> WiFiModule2 --> セサミを解錠する
}

```
