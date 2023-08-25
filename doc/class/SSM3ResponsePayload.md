
## SSM3ResponsePayload 类文档
```svg
internal class SSM3ResponsePayload(val data: ByteArray) {
    val cmdItCode: UByte = data[0].toUByte()
    val cmdResultCode: UByte = data[1].toUByte()
    val payload: ByteArray = data.drop(2).toByteArray()
}
```


### 描述

`SSM3ResponsePayload` 是一个Kotlin内部类，用于处理SSM3协议的响应负载（payload）。在这个类的实例化过程中，需要传入一个字节流数组。

### 属性

- `data`: 类型为 ByteArray 的实例变量。在创建类实例时需要提供这个 `data` 变量。

- `cmdItCode`: 将提供的 `data` 数组的第一个字节转换为 UByte（无符号字节）类型。

- `cmdResultCode`: 将提供的 `data` 数组的第二个字节转换为 UByte 类型。这通常用来表示操作的结果码。

- `payload`: 这是 `cmdResultCode` 之后的 `data` 的剩余部分。通过使用 `data.drop(2)` 生成一个新的数组，包含 `data` 中的所有元素，除了前两个。

### 使用

要使用这个类，你需要实例化一个 `