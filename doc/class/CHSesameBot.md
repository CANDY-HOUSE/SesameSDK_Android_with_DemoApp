
# CHSesameBot インターフェース
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
`CHSesameBot` は、`CHSesameLock` インターフェースを継承したデバイスのインターフェースであり、Sesame Bot デバイスの管理と操作に特化します。

## プロパティ

- `var mechSetting: CHSesameBotMechSettings?` - Sesame Bot の機械設定を保存します。

- `fun updateSetting(setting: CHSesameBotMechSettings, historyTag: ByteArray? = null, result: CHResult<CHEmpty>)` - Sesame Bot の設定を更新します。

- `fun toggle(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)` - Sesame Bot の状態を切り替えます。

- `fun lock(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)` - Sesame Botをロックします。

- `fun unlock(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)` - Sesame Botのロックを解除します。

- `fun click(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)` - Sesame Botをクリックするシミュレーションを行います。