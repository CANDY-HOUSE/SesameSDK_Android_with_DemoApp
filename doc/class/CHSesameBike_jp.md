# CHSesameBike インターフェースのドキュメント
```svg


interface CHSesameBike : CHSesameLock {
fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
}
```
`CHSesameBike` は、デバイス接口であり、`CHSesameLock` インターフェースを継承しています。Sesame Bike ロックデバイスの管理と操作を担当します。

## メソッド

- `fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - デバイスをアンロックします。操作が成功した場合、操作結果を返します。それ以外の場合はエラーメッセージを返します。
