# CHSesameSensor インターフェースのドキュメント
```svg

interface CHSesameSensor : CHSesameLock, CHSesameConnector {
fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun deleteCards(cardID: String, result: CHResult<CHEmpty>)
fun getCards(result: CHResult<ArrayList<String>>)
}
```
`CHSesameSensor` インターフェースは、Sesame Sensor デバイスの管理と操作に使用されます。このインターフェースは `CHSesameLock` および `CHSesameConnector` インターフェースを継承しています。

## メソッド

- `fun unlock(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)` - Sesame Sensor を解錠します。

- `fun deleteCards(cardID: String, result: CHResult<CHEmpty>)` - 指定したカードを削除します。

- `fun getCards(result: CHResult<ArrayList<String>>)` - すべてのカードを取得します。

上記は `CHSesameSensor` インターフェースの基本的な説明です。このインターフェースは Sesame Sensor デバイスに対して完全な操作と管理メソッドを提供します。
