# Bike 讲解
## 实现类 CHSesameTouchProDevice
### 接口

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

```
### 接口功能字义

####  [cards](../touch/card_get.md):获取卡片信息
####  [cardDelete](../touch/card_delete.md):卡片删除
####  [cardModeGet](../touch/card_model_get.md):获取卡片当前模式
####  [cardModeSet](../touch/card_model_set.md):设置卡片当前模式
####  [cardChange](../touch/card_change.md):修改卡片信息


####  [fingerPrints](../touch/finger_get.md):获取指纹信息
####  [fingerPrintModeGet](../touch/finger_mode_get.md):获取当前指纹信息
####  [fingerPrintModeSet](../touch/finger_mode_set.md):设置指纹信息
####  [fingerPrintDelete](../touch/finger_delete.md):删除指纹
####  [fingerPrintsChange](../touch/finger_change.md):修改指纹信息


####  [keyBoardPassCode](../touch/kbpc_get.md):获取密码锁
####  [keyBoardPassCodeChange](../touch/kbpc_change.md):修改密码锁
####  [keyBoardPassCodeDelete](../touch/kbpc_delete.md):删除密码锁
####  [keyBoardPassCodeModeGet](../touch/kbpc_mode_get.md):获取密码锁模式
####  [keyBoardPassCodeModeSet](../touch/kbpc_mode_set.md):设置密码锁模式

####  [insertSesame](../touch/add_sesame.md):获取sesame
####  [removeSesame](../touch/remove_sesame.md):删除sesame




