# # DeviceSegmentType 列挙型

```svg
internal enum class DeviceSegmentType(var value: Int) {
    plain(1), cipher(2), ;
    companion object {
        private val values = values()
        fun getByValue(value: Int) = values.first { it.value == value }
    }
}
```




`DeviceSegmentType` は内部の列挙型です。

## 列挙値

この列挙型には、多くの列挙項目が含まれており、それぞれ異なるデバイスのセグメントタイプに対応しています。各列挙項目は `Int` 型の値を持っています。

以下は`DeviceSegmentType` 列挙型の詳細なリストです。

| 命令名 | SegmentType | 説明 |
|:---:|:---:|:---|
| plain | 1 | 暗号化されていないデータ |
| cipher | 2 | 暗号化されているデータ |

## 伴生オブジェクト

`DeviceSegmentType` 列挙型には、次の機能を提供する伴生オブジェクトがあります。

- `values`：すべての値を返します。
- `getByValue`：指定された値によって対応の列挙項目を返します。対応の列挙項目が見つからない場合はエラーを返します。

