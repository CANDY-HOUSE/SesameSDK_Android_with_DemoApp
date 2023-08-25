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


`SesameResultCode` は、操作結果コードを表す内部の列挙型です。

## 列挙値

- `success`: 値は `0U` で、成功を示します。
- `invalidFormat`: 値は `1U` で、無効な形式を示します。
- `notSupported`: 値は `2U` で、サポートされていない操作を示します。
- `StorageFail`: 値は `3U` で、保存に失敗したことを示します。
- `invalidSig`: 値は `4U` で、無効な署名を示します。
- `notFound`: 値は `5U` で、見つからないことを示します。
- `UNKNOWN`: 値は `6U` で、未知のエラーを示します。
- `BUSY`: 値は `7U` で、デバイスが忙しいことを示します。
- `INVALID_PARAM`: 値は `8U` で、無効なパラメータを示します。

| 操作名 | 操作値 | 説明   |
| ------ | ------ | ------ |
| success | 0U | 成功   |
| invalidFormat | 1U | 無効な形式 |
| notSupported | 2U | サポートされていない操作 |
| StorageFail | 3U | 保存失敗 |
| invalidSig | 4U | 無効な署名 |
| notFound | 5U | 見つからない |
| UNKNOWN | 6U | 未知のエラー |
| BUSY | 7U | デバイスが忙しい |
| INVALID_PARAM | 8U | 無効なパラメータ |
上記は、SesameResultCode列挙型とその説明をMarkdown形式で表示したものです。この内容をコピーして、Markdownドキュメントに貼り付けてください。






