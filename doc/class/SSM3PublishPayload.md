## SSM3PublishPayload 类文档
```svg
class SSM3PublishPayload(val data: ByteArray) {
    val cmdItCode = data[0].toUByte()
    val payload: ByteArray = data.drop(1).toByteArray()
}
```
### 描述

`SSM3PublishPayload` 是一个Kotlin类，用于处理SSM3协议相关的 payload（负载）。在实例化这个类时，需要传入一个字节流数组（ByteArray）。

### 属性

- `data`：类型为ByteArray的实例变量。此变量值需要在创建类实例时提供。

- `cmdItCode`：将被提供的 `data` 数组的第一个字节转换为 `UByte` （无符号字节）。

- `payload`：这是在 `cmdItCode` 之后剩余的 `data` 部分。使用 `data.drop(1)`生成一个新的数组，其中包括 `data` 中的所有元素，除了第一个。

### 用法

要使用这个类，你需要实例化一个 `SSM3PublishPayload` 类并给它的构造方法提供一个字节流数组。这个数组的第一个字节会被转换成 `cmdItCode`，