## SSM2ResponsePayload 类文档
```svg
internal class SSM2ResponsePayload(val data: ByteArray) {
    val cmdItCode: UByte = data[0].toUByte()// login
    val cmdOPCode: UByte = data[1].toUByte()// sync
    val cmdResultCode: UByte = data[2].toUByte()// success
    val payload: ByteArray = data.drop(3).toByteArray()
}

```
### 描述

`SSM2ResponsePayload` 是一个Kotlin内部类，用于处理SSM2协议的响应载荷（payload）。在实例化此类时，需要传入一个字节流数组。

### 属性

- `data`: 一个 ByteArray 类型的实例变量。这个 `data` 变量应在创建类实例时提供。

- `cmdItCode`: 将提供的 `data` 数组的第一个字节转换为 UByte 类型（无符号字节）。这通常表示登录操作。

- `cmdOPCode`: 将提供的 `data` 数组的第二个字节转换为 UByte 类型。这通常表示同步操作。

- `cmdResultCode`: 将提供的 `data` 数组的第三个字节转换为 UByte 类型。这通常用来表示操作是否成功。

- `payload`: 这是在 `cmdResultCode` 之后 `data` 的剩余部分。通过 `data.drop(3)`