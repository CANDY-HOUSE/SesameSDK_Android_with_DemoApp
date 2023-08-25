
# SesameOS3Payload 数据类
```svg
internal data class SesameOS3Payload(val itemCode: UByte, val data: ByteArray) {
    fun toDataWithHeader(): ByteArray {
        return byteArrayOf(itemCode.toByte()) + data
    }
}


```
`SesameOS3Payload`数据类主要用于处理和管理Sesame OS3的有效载荷数据。

## 属性

- `itemCode`：项代码，UByte类型。
- `data`：字节数组，ByteArray类型，存储有效载荷数据。

## 方法

- `toDataWithHeader()`：此方法将`itemCode`转换为Byte类型，并且将其作为头部与`data`连接成新的字节数组。这种方法主要用于在发送或存储有效载荷数据时，将其转换为适合的格式。
