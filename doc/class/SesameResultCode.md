# SesameResultCode 枚举类
```svg
internal enum class SesameResultCode(val value: UByte) {
    success(0U) 
    invalidFormat(1U) 
    notSupported(2U)
    StorageFail(3U) 
    invalidSig(4U) 
    notFound(5U) 
    UNKNOWN(6U)
    BUSY(7U)
    INVALID_PARAM(8U)
}
```


`SesameResultCode` 是一个内部枚举类。

## 枚举值

- `success`: 值为 `0U`，表示成功。
- `invalidFormat`: 值为 `1U`，表示无效的格式。
- `notSupported`: 值为 `2U`，表示不支持的操作。
- `StorageFail`: 值为 `3U`，表示存储失败。
- `invalidSig`: 值为 `4U`，表示无效的签名。
- `notFound`: 值为 `5U`，表示未找到。
- `UNKNOWN`: 值为 `6U`，表示未知错误。
- `BUSY`: 值为 `7U`，表示设备忙碌。
- `INVALID_PARAM`: 值为 `8U`，表示无效的参数。


  | 指令名称 | 指令值 |  描述  |
  | :---: |:----:| :---: |
  | success | 0U |  成功  |
  | invalidFormat | 1U | 无效格式 |
  | notSupported | 2U | 不支持  |
  | StorageFail | 3U | 存储失败 |
  | invalidSig | 4U | 无效签名 |
  | notFound | 5U | 未找到  |
  | UNKNOWN | 6U |  未知  |
  | BUSY | 7U |  忙碌  |
  | INVALID_PARAM | 8U | 无效参数 |
