
# SesameOS3Payload データのクラス
```svg
internal data class SesameOS3Payload(val itemCode: UByte, val data: ByteArray) {
    fun toDataWithHeader(): ByteArray {
        return byteArrayOf(itemCode.toByte()) + data
    }
}


```
`SesameOS3Payload`データのクラスは、Sesame OS3 のペイロードデータを処理および管理します。

## プロパティ

- `itemCode`：UByte型です。
- `data`：有効なペイロードデータを保存するByteArray型です。

## メソッド

- `toDataWithHeader()`：このメソッドは、`itemCode` を Byte 型に変換し、それをヘッダーとして `data` と連結して新しいバイト配列を生成します。このメソッドは、有効なペイロードデータを送信または保存する際に、適切な形式に変換します。