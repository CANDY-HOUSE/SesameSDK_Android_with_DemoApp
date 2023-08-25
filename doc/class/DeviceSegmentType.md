# # DeviceSegmentType 枚举类

```svg
internal enum class DeviceSegmentType(var value: Int) {
    plain(1), cipher(2), ;
    companion object {
        private val values = values()
        fun getByValue(value: Int) = values.first { it.value == value }
    }
}
```




`DeviceSegmentType` 是一个内部枚举类。

## 枚举值

这个枚举类包含了多个枚举项，表示设备段类型。每个枚举项都有一个 `Int` 类型的值。

以下是对 `DeviceSegmentType` 枚举类的详细列表：

| 枚举值 | 值 | 描述 |
|---|---|---|
| plain | 1 | 明文 |
| cipher | 2 | 密文 |

## 伴生对象

`DeviceSegmentType` 枚举类有一个伴生对象，提供了以下功能：

- `values`：返回所有的枚举值。
- `getByValue`：根据给定的值返回对应的枚举项。如果没有找到对应的枚举项，抛出异常。

