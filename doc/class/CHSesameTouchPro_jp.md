# CHSesameTouchPro インターフェースのドキュメント
```svg
interface CHSesameTouchPro : CHSesameConnector {

    fun cards(result: CHResult<CHEmpty>)
    fun cardDelete(ID: String, result: CHResult<CHEmpty>)
    fun cardChange(ID: String, name: String, result: CHResult<CHEmpty>)
    fun cardModeGet(result: CHResult<Byte>)
    fun cardModeSet(mode: Byte, result: CHResult<CHEmpty>)

    fun fingerPrints(result: CHResult<CHEmpty>)
    fun fingerPrintDelete(ID: String, result: CHResult<CHEmpty>)
    fun fingerPrintsChange(ID: String, name: String, result: CHResult<CHEmpty>)
    fun fingerPrintModeGet(result: CHResult<Byte>)
    fun fingerPrintModeSet(mode: Byte, result: CHResult<CHEmpty>)

    fun keyBoardPassCode(result: CHResult<CHEmpty>)
    fun keyBoardPassCodeChange(ID: String, name: String, result: CHResult<CHEmpty>)
    fun keyBoardPassCodeDelete(ID: String, result: CHResult<CHEmpty>)
    fun keyBoardPassCodeModeGet(result: CHResult<Byte>)
    fun keyBoardPassCodeModeSet(mode: Byte, result: CHResult<CHEmpty>)

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
