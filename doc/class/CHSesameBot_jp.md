# CHSesameBot インターフェースのドキュメント
```svg

interface CHSesameBot : CHSesameLock {
var mechSetting: CHSesameBotMechSettings?
fun updateSetting(setting: CHSesameBotMechSettings, historyTag: ByteArray? = null, result: CHResult<CHEmpty>)
fun toggle(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)
fun lock(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)
fun unlock(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)
fun click(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)
}
```
`CHSesameBot` は、デバイス接口であり、`CHSesameLock` インターフェースを継承しています。Sesame Bot デバイスの管理と操作を担当します。

## 属性

- `var mechSetting: CHSesameBotMechSettings?` - Sesame Bot の機械設定を保持するためのプロパティ。

## メソッド

- `fun updateSetting(setting: CHSesameBotMechSettings, historyTag: ByteArray? = null, result: CHResult<CHEmpty>)` - Sesame Bot の設定を更新します。

- `fun toggle(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)` - Sesame Bot の状態を切り替えます。

- `fun lock(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)` - Sesame Bot を施錠します。

- `fun unlock(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)` - Sesame Bot を解錠します。

- `fun click(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)` - Sesame Bot にクリック操作をシミュレートします。
