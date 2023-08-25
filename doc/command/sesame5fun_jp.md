# Sesame5 機能コマンド
## 実装クラス: CHSesame5Device

### インターフェース

```agsl
fun lock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun unlock(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun toggle(historytag: ByteArray? = null, result: CHResult<CHEmpty>)
fun magnet(result: CHResult<CHEmpty>)
fun configureLockPosition(lockTarget: Short, unlockTarget: Short, result: CHResult<CHEmpty>)
fun autolock(delay: Int, result: CHResult<Int>)
fun history(cursor: Long?, uuid: UUID, result: CHResult<Pair<List<CHSesame5History>, Long?>>)
fun reset(result: CHResult<CHEmpty>)
fun getVersionTag(result: CHResult<CHEmpty>)
```
### インターフェースの機能説明

#### 1. [lock](lock_jp.md):ロックする 
#### 2. [unlock](unlock_jp.md):ロックを解除する 
#### 3. toggle:ロックとアンロックを切り替える
#### 4. [magnet](magnet_jp.md):アングルキャリブレーション 
#### 5. [configureLockPosition](configureLockPosition_jp.md):ロックとアンロックの位置を設定する 
#### 6. [ autolock ](autolock_jp.md):自動ロック
- delay:遅延時間
#### 7. [history](history_jp.md):履歴を取得する
#### 8. [reset](reset_jp.md):デバイスをリセットする
#### 9. [getVersionTag](ssm5version_jp.md):バージョンタグを取得する

### フローチャート
![CHSesame5Device](../class/CHSesame5Device.svg)





