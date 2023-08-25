# CHSesame5 インターフェースのドキュメント
```svg

interface CHSesame5 : CHSesameLock {
var mechSetting: CHSesame5MechSettings?
fun lock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun toggle(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun magnet(result: CHResult<CHEmpty>)
fun configureLockPosition(lockTarget: Short, unlockTarget: Short, result: CHResult<CHEmpty>)
fun autolock(delay: Int, result: CHResult<Int>)
fun history(cursor: Long?, uuid: UUID, result: CHResult<Pair<List<CHSesame5History>, Long?>>)
}
```
`CHSesame5` は、デバイス接口であり、`CHSesameLock` インターフェースを継承しています。Sesame5 ロックデバイスの管理と操作を担当します。

## 属性

- `mechSetting: CHSesame5MechSettings?` - Sesame5 ロックの機械設定を取得または設定します。

## メソッド

- `fun lock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - デバイスをロックします。操作が成功した場合、操作結果を返します。それ以外の場合はエラーメッセージを返します。
- `fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - デバイスをアンロックします。操作が成功した場合、操作結果を返します。それ以外の場合はエラーメッセージを返します。
- `fun toggle(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - デバイスのロック状態を切り替えます。操作が成功した場合、操作結果を返します。それ以外の場合はエラーメッセージを返します。
- `fun magnet(result: CHResult<CHEmpty>)` - デバイスに磁力操作を行います。操作が成功した場合、操作結果を返します。それ以外の場合はエラーメッセージを返します。
- `fun configureLockPosition(lockTarget: Short, unlockTarget: Short, result: CHResult<CHEmpty>)` - ロックの位置を設定します。操作が成功した場合、操作結果を返します。それ以外の場合はエラーメッセージを返します。
- `fun autolock(delay: Int, result: CHResult<Int>)` - 自動ロックの遅延時間を設定します。操作が成功した場合、操作結果を返します。それ以外の場合はエラーメッセージを返します。
- `fun history(cursor: Long?, uuid: UUID, result: CHResult<Pair<List<CHSesame5History>, Long?>>)` - デバイスの履歴を取得します。操作が成功した場合、操作結果を返します。それ以外の場合はエラーメッセージを返します。
