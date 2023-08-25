
# CHSesameTouchPro 接口
```svg
interface CHSesameTouchPro : CHSesameConnector {

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

}
```
`CHSesameTouchPro` 是一个设备接口，继承自 `CHSesameConnector` 接口。这个接口专门用来管理和操作 Sesame Touch Pro 设备。

## 方法

### 卡片相关:

- `fun cards(result: CHResult<CHEmpty>)` - 获取所有的卡片。

- `fun cardDelete(ID: String, result: CHResult<CHEmpty>)` - 删除指定的卡片。

- `fun cardChange(ID: String, name: String, result: CHResult<CHEmpty>)` - 更改指定卡片的名字。

- `fun cardModeGet(result: CHResult<Byte>)` - 获取卡片模式。

- `fun cardModeSet(mode: Byte, result: CHResult<CHEmpty>)` - 设置卡片模式。

### 指纹相关:

- `fun fingerPrints(result: CHResult<CHEmpty>)` - 获取所有的指纹。

- `fun fingerPrintDelete(ID: String, result: CHResult<CHEmpty>)` - 删除指定的指纹。

- `fun fingerPrintsChange(ID: String, name: String, result: CHResult<CHEmpty>)` - 更改指定指纹的名字。

- `fun fingerPrintModeGet(result: CHResult<Byte>)` - 获取指纹模式。

- `fun fingerPrintModeSet(mode: Byte, result: CHResult<CHEmpty>)` - 设置指纹模式。

### 密码相关:

- `fun keyBoardPassCode(result: CHResult<CHEmpty>)` - 获取所有的密码。

- `fun keyBoardPassCodeChange(ID: String, name: String, result: CHResult<CHEmpty>)` - 更改指定密码的名字。

- `fun keyBoardPassCodeDelete(ID: String, result: CHResult<CHEmpty>)` - 删除指定的密码。

- `fun keyBoardPassCodeModeGet(result: CHResult<Byte>)` - 获取密码模式。

- `fun keyBoardPassCodeModeSet(mode: Byte, result: CHResult<CHEmpty>)` - 设置密码模式。

以上是 `CHSesameTouchPro` 接口的基本描述，这个接口为 Sesame Touch Pro 设备提供了一套完整的操作和管理方法。
