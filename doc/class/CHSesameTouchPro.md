
# CHSesameTouchPro インターフェース
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
`CHSesameTouchPro` は`CHSesameConnector`インターフェースを継承し、デバイスのインターフェースです。 Sesame Touch Proデバイスの管理と操作に特化しています。

## メソッド

### カード:

- `fun cards(result: CHResult<CHEmpty>)` - すべてのカードを取得します。

- `fun cardDelete(ID: String, result: CHResult<CHEmpty>)` - 指定されたカードを削除します。

- `fun cardChange(ID: String, name: String, result: CHResult<CHEmpty>)` - 指定されたカードの名称を変更します。

- `fun cardModeGet(result: CHResult<Byte>)` - カードモデルを取得します。

- `fun cardModeSet(mode: Byte, result: CHResult<CHEmpty>)` - カードモデルを設置します。

### 指紋:

- `fun fingerPrints(result: CHResult<CHEmpty>)` - すべての指紋を取得します。

- `fun fingerPrintDelete(ID: String, result: CHResult<CHEmpty>)` - 指定された指紋を削除します。

- `fun fingerPrintsChange(ID: String, name: String, result: CHResult<CHEmpty>)` - 指定された指紋の名称を変更します。

- `fun fingerPrintModeGet(result: CHResult<Byte>)` - 指紋モデルを取得します。

- `fun fingerPrintModeSet(mode: Byte, result: CHResult<CHEmpty>)` - 指紋モデルを設置します。

### パスワード:

- `fun keyBoardPassCode(result: CHResult<CHEmpty>)` - すべてのパスワードを取得します。

- `fun keyBoardPassCodeChange(ID: String, name: String, result: CHResult<CHEmpty>)` - 指定されたパスワードの名称を変更します。

- `fun keyBoardPassCodeDelete(ID: String, result: CHResult<CHEmpty>)` - 指定されたパスワードを削除します。

- `fun keyBoardPassCodeModeGet(result: CHResult<Byte>)` - パスワードモデルを取得します。

- `fun keyBoardPassCodeModeSet(mode: Byte, result: CHResult<CHEmpty>)` - パスワードモデルを設置します。

以上は `CHSesameTouchPro`インタフェースについての基本的な説明です。Sesame Touch Proデバイスに対して完全な操作と管理方法を提供します。
