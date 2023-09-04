
# CHSesame5 インターフェース

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

`CHSesame5` は `CHSesameLock` インターフェースを継承したデバイスインターフェースで、Sesame5 ロックのデバイスの管理と操作を行います。
## プロパティ

- `mechSetting: CHSesame5MechSettings?` - Sesame5 の機械設置の取得または設置を行います。

## メソッド

- `fun lock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - デバイスをロックします。操作が成功した場合は操作結果を返し、それ以外の場合はエラーメッセージを返します。
- `fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - デバイスのロックを解除します。操作が成功した場合は操作結果を返し、それ以外の場合はエラーメッセージを返します。
- `fun toggle(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - デバイスのロック状態を切替えます。操作が成功した場合は操作結果を返し、それ以外の場合はエラーメッセージを返します。
- `fun magnet(result: CHResult<CHEmpty>)` - デバイスに磁化処理を行います。操作が成功した場合は操作結果を返し、それ以外の場合はエラーメッセージを返します。
- `fun configureLockPosition(lockTarget: Short, unlockTarget: Short, result: CHResult<CHEmpty>)` - ロックの位置を設置します。操作が成功した場合は操作結果を返し、それ以外の場合はエラーメッセージを返します。
- `fun autolock(delay: Int, result: CHResult<Int>)` - 自動ロックの遅延時間を設定します。操作が成功した場合は操作結果を返し、それ以外の場合はエラーメッセージを返します。
- `fun history(cursor: Long?, uuid: UUID, result: CHResult<Pair<List<CHSesame5History>, Long?>>)` - デバイスの履歴記録を取得します。操作が成功した場合は操作結果を返し、それ以外の場合はエラーメッセージを返します。

