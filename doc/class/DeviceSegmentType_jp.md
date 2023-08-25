# DeviceSegmentType 列挙型

```svg
internal enum class DeviceSegmentType(var value: Int) {
    plain(1), cipher(2), ;
    companion object {
        private val values = values()
        fun getByValue(value: Int) = values.first { it.value == value }
    }
}
```
`DeviceSegmentType` は、デバイスセグメントのタイプを示す内部の列挙型です。

## 列挙値

この列挙型は、複数の列挙項目を含んでおり、デバイスセグメントのタイプを表します。各列挙項目は `Int` 型の値を持ちます。

以下は `DeviceSegmentType` 列挙型の詳細な一覧です：

| 列挙値 | 値 | 説明 |
|---|---|---|
| plain | 1 | 平文 |
| cipher | 2 | 暗号文 |

## 伴生オブジェクト

`DeviceSegmentType` 列挙型には、以下の機能を提供する伴生オブジェクトがあります：

- `values`：すべての列挙値を返します。
- `getByValue`：与えられた値に対応する列挙項目を返します。対応する列挙項目が見つからない場合、例外がスローされます。
