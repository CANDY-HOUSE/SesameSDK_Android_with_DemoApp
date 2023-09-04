
# CHSesameBike2 インターフェイス
```svg

interface CHSesameBike2 : CHSesameLock {

fun unlock(tag: ByteArray? = null, result: CHResult<CHEmpty>)
}

```
`CHSesameBike2` は`CHSesameLock` インターフェースを継承したデバイスインターフェースで、Sesame Bike2 ロックのデバイスの管理と操作を行います。

## メソッド

- `fun unlock(tag: ByteArray? = null, result: CHResult<CHEmpty>)` - デバイスのロックを解除します。操作が成功した場合は操作結果を返し、それ以外の場合はエラーメッセージを返します。

