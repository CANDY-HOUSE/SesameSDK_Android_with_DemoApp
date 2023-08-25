
# CHSesameTouchCard 类
```svg
class CHSesameTouchCard(data: ByteArray) {
val cardType = data[0]
val idLength = data[1]
val cardID = data.sliceArray(2..idLength + 1).toHexString()
val nameIndex = idLength + 2
val nameLength = data[nameIndex]
val cardName = String(data.sliceArray(nameIndex + 1..nameIndex + nameLength))
}

```
`CHSesameTouchCard`类主要用于处理和管理触摸卡的信息。这个类的构造函数接收一个ByteArray作为参数，该参数包含了触摸卡的相关数据。

## 属性

- `cardType`：卡片类型，由数据的第一个字节表示。
- `idLength`：卡片ID的长度，由数据的第二个字节表示。
- `cardID`：卡片ID，由数据的第三个字节开始，长度为`idLength`的一段数据表示，然后转换为十六进制字符串。
- `nameIndex`：卡片名称数据的起始索引，由`idLength + 2`计算得出。
- `nameLength`：卡片名称的长度，由数据的`nameIndex`字节表示。
- `cardName`：卡片名称，由数据的`nameIndex + 1`开始，长度为`nameLength`的一段数据表示，然后转换为字符串。

## 构造函数

- `CHSesameTouchCard(data: ByteArray)`：构造函数接收一个ByteArray作为参数，该参数包含了触摸卡的相关数据。在构造函数中，将解析这些数据，并初始化相应的属性。
