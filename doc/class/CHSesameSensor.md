
# CHSesameSensor インターフェース
```svg

interface CHSesameSensor : CHSesameLock, CHSesameConnector {
fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun deleteCards(cardID: String, result: CHResult<CHEmpty>)
fun getCards(result: CHResult<ArrayList<String>>)
}
```

`CHSesameSensor`は、`CHSesameLock`および`CHSesameConnector`インターフェースを継承したデバイスインターフェースです。Sesame Sensorデバイスの管理と操作に特化しています。

## メソッド

- `fun unlock(historyTag: ByteArray? = null, result: CHResult<CHEmpty>)` - Sesame Sensorのロックを解除します。

- `fun deleteCards(cardID: String, result: CHResult<CHEmpty>)` - 指定されたカードを削除します。

- `fun getCards(result: CHResult<ArrayList<String>>)` - すべてのカードを取得します。

上記は`CHSesameSensor`インターフェースについての基本的な説明です。Sesame Sensorデバイスに対して完全な操作と管理方法を提供します。
