

# CHSesame2 インターフェイス
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
`CHSesame2`は`CHSesameLock`インターフェースを継承したデバイスインターフェースで、Sesame2 ロックのデバイスの管理と操作を行います。

## プロパティ

- `mechSetting: CHSesame2MechSettings?` -  Sesame2 の機械設置の取得または設置を行います。

## メソッド

- `fun lock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - デバイスをロックします。操作が成功した場合は操作結果が返され、それ以外の場合はエラーメッセージが返されます。
- `fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - デバイスのロックを解除します。操作が成功した場合は操作結果が返され、それ以外の場合はエラーメッセージが返されます。
- `fun toggle(historytag: ByteArray? = null, result: CHResult<CHEmpty>)` - デバイスのロック状態を切替えます。操作が成功した場合は操作結果が返され、それ以外の場合はエラーメッセージが返されます。
- `fun configureLockPosition(lockTarget: Short, unlockTarget: Short, result: CHResult<CHEmpty>)` - ロックの位置を設置します。操作が成功した場合は操作結果が返され、それ以外の場合はエラーメッセージが返されます。
- `fun getAutolockSetting(result: CHResult<Int>)` - 自動ロックの設定を取得します。操作が成功した場合は操作結果が返され、それ以外の場合はエラーメッセージが返されます。
- `fun enableAutolock(delay: Int, historytag: ByteArray? = null, result: CHResult<Int>)` - 自動ロックを有効にします。操作が成功した場合は操作結果が返され、それ以外の場合はエラーメッセージが返されます。
- `fun disableAutolock(historytag: ByteArray? = null, result: CHResult<Int>)` - 自動ロックを無効にします。操作が成功した場合は操作結果が返され、それ以外の場合はエラーメッセージが返されます。
- `fun getHistories(cursor: Long?, result: CHResult<Pair<List<CHSesame2History>, Long?>>)` - デバイスの履歴記録を取得します。操作が成功した場合は操作結果が返され、それ以外の場合はエラーメッセージが返されます。

