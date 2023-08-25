# CHSesame2 インターフェースのドキュメント
```svg
interface CHSesame2 : CHSesameLock { // CHProductModel.SS2,CHProductModel.SS4
var mechSetting: CHSesame2MechSettings?
fun lock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun toggle(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun configureLockPosition(lockTarget: Short, unlockTarget: Short, result: CHResult<CHEmpty>)
fun getAutolockSetting(result: CHResult<Int>)
fun enableAutolock(delay: Int, historytag: ByteArray? = null, result: CHResult<Int>)
fun disableAutolock(historytag: ByteArray? = null, result: CHResult<Int>)
fun getHistories(cursor: Long?, result: CHResult<Pair<List<CHSesame2History>, Long?>>)
}

```
`CHSesame2` は、デバイス接口であり、`CHSesameLock` インターフェースを継承しています。Sesame2 ロックデバイスの管理と操作を担当します。

## 属性

- `mechSetting: CHSesame2MechSettings?` - Sesame2 ロックの機械設定を取得または設定します。

## メソッド

- `fun lock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - デバイスをロックします。操作が成功した場合、操作結果を返します。それ以外の場合はエラーメッセージを返します。
- `fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - デバイスをアンロックします。操作が成功した場合、操作結果を返します。それ以外の場合はエラーメッセージを返します。
- `fun toggle(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - デバイスのロック状態を切り替えます。操作が成功した場合、操作結果を返します。それ以外の場合はエラーメッセージを返します。
- `fun configureLockPosition(lockTarget: Short, unlockTarget: Short, result: CHResult<CHEmpty>)` - ロックの位置を設定します。操作が成功した場合、操作結果を返します。それ以外の場合はエラーメッセージを返します。
- `fun getAutolockSetting(result: CHResult<Int>)` - 自動ロックの設定を取得します。操作が成功した場合、操作結果を返します。それ以外の場合はエラーメッセージを返します。
- `fun enableAutolock(delay: Int, historytag: ByteArray? = null, result: CHResult<Int>)` - 自動ロックを有効にします。操作が成功した場合、操作結果を返します。それ以外の場合はエラーメッセージを返します。
- `fun disableAutolock(historytag: ByteArray? = null, result: CHResult<Int>)` - 自動ロックを無効にします。操作が成功した場合、操作結果を返します。それ以外の場合はエラーメッセージを返します。
- `fun getHistories(cursor: Long?, result: CHResult<Pair<List<CHSesame2History>, Long?>>)` - デバイスの履歴を取得します。操作が成功した場合、操作結果を返します。それ以外の場合はエラーメッセージを返します。
