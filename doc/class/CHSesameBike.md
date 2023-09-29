
# CHSesameBike インターフェイス
```svg


interface CHSesameBike : CHSesameLock {
fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
}
```
`CHSesameBike` は `CHSesameLock` インターフェースを継承したデバイスインターフェースで、Sesame Bike ロックのデバイスの管理と操作を行います。

## メソッド

- `fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - デバイスのロックを解除します。操作が成功した場合は操作結果を返し、それ以外の場合はエラーメッセージを返します。

