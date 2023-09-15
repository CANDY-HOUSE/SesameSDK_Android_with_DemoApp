#   CHSesameTouchProDevice  説明
## 実装クラス 
### インターフェース

```agsl
    fun cards(result: CHResult<CHEmpty>)
    fun cardDelete(ID: String, result: CHResult<CHEmpty>)
    fun cardChange(ID: String, name: String, result: CHResult<CHEmpty>)
    fun cardModeGet(result: CHResult<Byte>)
    fun cardModeSet(mode: Byte, result: CHResult<CHEmpty>)

    fun fingerPrints(result: CHResult<CHEmpty>)
    fun fingerPrintDelete(ID: String, result: CHResult<CHEmpty>)
    fun fingerPrintsChange(ID: String, name: String, result: CHResult<CHEmpty>)
    fun fingerPrintModeGet(result: CHResult<Byte>)
    fun fingerPrintModeSet(mode: Byte, result: CHResult<CHEmpty>)

    fun keyBoardPassCode(result: CHResult<CHEmpty>)
    fun keyBoardPassCodeChange(ID: String, name: String, result: CHResult<CHEmpty>)
    fun keyBoardPassCodeDelete(ID: String, result: CHResult<CHEmpty>)
    fun keyBoardPassCodeModeGet(result: CHResult<Byte>)
    fun keyBoardPassCodeModeSet(mode: Byte, result: CHResult<CHEmpty>)
    
    
    fun insertSesame(sesame: CHDevices, result: CHResult<CHEmpty>)
    fun removeSesame(tag: String, result: CHResult<CHEmpty>)
```
### インターフェースの機能の定義

####  [cards](../touch/card_get.md):カードの情報を取得する
####  [cardDelete](../touch/card_delete.md):カードを削除する
####  [cardModeGet](../touch/card_model_get.md):カードのモデルを取得する
####  [cardModeSet](../touch/card_model_set.md):カードのモデルを設置する
####  [cardChange](../touch/card_change.md):カードの情報を変更する


####  [fingerPrints](../touch/finger_get.md):指紋情報を取得する
####  [fingerPrintModeGet](../touch/finger_mode_get.md):指紋のモデルを取得する
####  [fingerPrintModeSet](../touch/finger_mode_set.md):指紋のモデルを設置する
####  [fingerPrintDelete](../touch/finger_delete.md):指紋を削除する
####  [fingerPrintsChange](../touch/finger_change.md):指紋の情報を変更する


####  [keyBoardPassCode](../touch/kbpc_get.md):パスワードロックを取得する
####  [keyBoardPassCodeChange](../touch/kbpc_change.md):パスワードロックを変更する
####  [keyBoardPassCodeDelete](../touch/kbpc_delete.md):パスワードロックを削除する
####  [keyBoardPassCodeModeGet](../touch/kbpc_mode_get.md):パスワードロックのモデルを取得する
####  [keyBoardPassCodeModeSet](../touch/kbpc_mode_set.md):パスワードロックのモデルを設置する


####  [insertSesame](../touch/add_sesame.md):sesameを取得する
####  [removeSesame](../touch/remove_sesame.md):sesameを削除する


### フローチャート
![CHSesameTouchProDevice](../class/CHSesameTouchProDevice.svg)





