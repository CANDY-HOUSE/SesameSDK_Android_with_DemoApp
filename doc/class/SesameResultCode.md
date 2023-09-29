# SesameResultCode 列挙型
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


`SesameResultCode` は、内部の列挙型です。

## 列挙値

- `success`: 値が `0U`で、成功を指します。
- `invalidFormat`: 値が `1U`で、無効なフォーマットを指します。
- `notSupported`: 値が `2U`で、対応不可を指します。
- `StorageFail`: 値が `3U`で、保存失敗を指します。
- `invalidSig`: 値が `4U`で、無効な署名を指します。
- `notFound`: 値が `5U`で、見つからないことを指します。
- `UNKNOWN`: 値が `6U`で、不明なエラーを指します。
- `BUSY`: 値が `7U`で、デバイスがビジーであることを指します。
- `INVALID_PARAM`: 値が `8U`で、無効なパラメータを指します。


  | 命令名 | SesameResultCode |  説明  |
  | :---: |:----:| :---: |
  | success | 0U |  成功  |
  | invalidFormat | 1U | 無効なフォーマット |
  | notSupported | 2U | 対応不可  |
  | StorageFail | 3U | 保存失敗 |
  | invalidSig | 4U | 無効な署名 |
  | notFound | 5U | 見つからないこと  |
  | UNKNOWN | 6U |  不明  |
  | BUSY | 7U |  ビジー  |
  | INVALID_PARAM | 8U | 無効なパラメータ |
