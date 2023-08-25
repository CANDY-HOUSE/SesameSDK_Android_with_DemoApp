# CHSesameBike2 インターフェースのドキュメント
```svg

interface CHSesameBike2 : CHSesameLock {

fun unlock(tag: ByteArray? = null, result: CHResult<CHEmpty>)
}

```
`CHSesameBike2` は、デバイス接口であり、`CHSesameLock` インターフェースを継承しています。Sesame Bike2 ロックデバイスの管理と操作を担当します。

## メソッド

- `fun unlock(tag: ByteArray? = null, result: CHResult<CHEmpty>)` - デバイスをアンロックします。操作が成功した場合、操作結果を返します。それ以外の場合はエラーメッセージを返します。
