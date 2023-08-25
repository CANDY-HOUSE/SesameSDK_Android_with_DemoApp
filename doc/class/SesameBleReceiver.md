# SesameBleReceiver 类

```svg


internal class SesameBleReceiver {
    var buffer = byteArrayOf()
    internal fun feed(input: ByteArray): Pair<DeviceSegmentType, ByteArray>? 
   
}
```



`SesameBleReceiver` 是一个内部类。

## 属性

- `buffer`：一个字节数组，用于存储接收到的蓝牙数据。

## 方法

- `feed(input: ByteArray): Pair<DeviceSegmentType, ByteArray>?`：这个方法接受一个字节数组作为输入，然后返回一个包含 `DeviceSegmentType` 和字节数组的二元组。如果处理失败，返回 `null`。
