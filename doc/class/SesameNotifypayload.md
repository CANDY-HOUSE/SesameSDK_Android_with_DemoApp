## SesameNotifypayload 类文档
```
internal class SesameNotifypayload(val data: ByteArray) {
    val notifyOpCode: SSM2OpCode = SSM2OpCode.getByValue(data[0])
    val payload: ByteArray = data.drop(1).toByteArray()
}
```
### 描述

`SesameNotifypayload`是一个Kotlin内部类，用于处理'Sesame'通知的负载（payload）。在实例化此类时，需要传入一个字节流数组。

### 属性

- `data`: 一个 ByteArray 类型的实例变量。这个 `data` 变量应当在创建类实例时提供。

- `notifyOpCode`: 会将提供的`data`数组的第一个字节获取其值，传递给`SSM2OpCode.getByValue`函数，并将得到的结果作为`notifyOpCode`。

- `payload`: 这是`notifyOpCode`后`data`的其余部分。通过`data.drop(1)`会生成一个新的数组，包括`data`的所有元素，除了第一个。

### 使用

要使用这个类，你需要实例化一个`SesameNotifypayload`对象，并向其构造函数提供一个字节流数组。这个数组的第一个字节