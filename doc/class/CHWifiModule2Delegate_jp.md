# CHSesameTouchPro インターフェースのドキュメント
```
interface CHWifiModule2Delegate : CHDeviceStatusDelegate {
fun onAPSettingChanged(device: CHWifiModule2, settings: CHWifiModule2MechSettings) {}
//    fun onNetWorkStatusChanged(device: CHWifiModule2, settings: CHWifiModule2NetWorkStatus) {}
fun onSSM2KeysChanged(device: CHWifiModule2, ssm2keys: Map<String, String>) {}
fun onOTAProgress(device: CHWifiModule2, percent: Byte) {}
fun onScanWifiSID(device: CHWifiModule2, ssid: String, rssi: Short) {}
}
```
`CHSesameTouchPro` インターフェースは、Sesame Touch Pro デバイスの管理と操作に使用されます。このインターフェースは `CHSesameConnector` インターフェースを継承しています。

## メソッド

### カード関連:

- `fun cards(result: CHResult<CHEmpty>)` - すべてのカードを取得します。

- `fun cardDelete(ID: String, result: CHResult<CHEmpty>)` - 指定したカードを削除します。

- `fun cardChange(ID: String, name: String, result: CHResult<CHEmpty>)` - 指定したカードの名前を変更します。

- `fun cardModeGet(result: CHResult<Byte>)` - カードモードを取得します。

- `fun cardModeSet(mode: Byte, result: CHResult<CHEmpty>)` - カードモードを設定します。

### 指紋関連:

- `fun fingerPrints(result: CHResult<CHEmpty>)` - すべての指紋を取得します。

- `fun fingerPrintDelete(ID: String, result: CHResult<CHEmpty>)` - 指定した指紋を削除します。

- `fun fingerPrintsChange(ID: String, name: String, result: CHResult<CHEmpty>)` - 指定した指紋の名前を変更します。

- `fun fingerPrintModeGet(result: CHResult<Byte>)` - 指紋モードを取得します。

- `fun fingerPrintModeSet(mode: Byte, result: CHResult<CHEmpty>)` - 指紋モードを設定します。

### パスコード関連:

- `fun keyBoardPassCode(result: CHResult<CHEmpty>)` - すべてのパスコードを取得します。

- `fun keyBoardPassCodeChange(ID: String, name: String, result: CHResult<CHEmpty>)` - 指定したパスコードの名前を変更します。

- `fun keyBoardPassCodeDelete(ID: String, result: CHResult<CHEmpty>)` - 指定したパスコードを削除します。

- `fun keyBoardPassCodeModeGet(result: CHResult<Byte>)` - パスコードモードを取得します。

- `fun keyBoardPassCodeModeSet(mode: Byte, result: CHResult<CHEmpty>)` - パスコードモードを設定します。

上記は `CHSesameTouchPro` インターフェースの基本的な説明です。このインターフェースは Sesame Touch Pro デバイスに対して完全な操作と管理メソッドを提供します。
