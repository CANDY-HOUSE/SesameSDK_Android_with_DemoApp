# SesameBleTransmit 类

```svg

internal class SesameBleTransmit(var type: DeviceSegmentType, var input: ByteArray) {
    var isStart = 1
    internal fun getChunk(): ByteArray?
}

```

`SesameBleTransmit` 是一个内部类。

## 属性

- `type`：一个 `DeviceSegmentType` 枚举值，表示传输的数据类型。
- `input`：一个字节数组，表示需要传输的数据。
- `isStart`：一个整型值，用于标记数据传输的开始。初始值为 1，传输开始后设置为 0，传输结束后设置为 -1。

## 方法

- `getChunk(): ByteArray?`：此方法返回一个字节数组，表示一个数据块。如果 `isStart` 为 -1，表示数据已经全部传输完毕，此时返回 `null`。如果 `input` 的大小小于或等于 19，那么会一次性将所有数据进行传输，并将 `isStart` 设置为 -1。如果 `input` 的大小大于 19，那么会每次传输 19 个字节的数据，直到所有数据都被传输完毕。
