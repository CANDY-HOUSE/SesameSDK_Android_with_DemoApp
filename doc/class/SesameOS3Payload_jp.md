# SesameOS3Payload クラスのドキュメント
```svg
internal data class SesameOS3Payload(val itemCode: UByte, val data: ByteArray) {
    fun toDataWithHeader(): ByteArray {
        return byteArrayOf(itemCode.toByte()) + data
    }
}


```
`SesameOS3Payload` クラスは、Sesame OS3 のペイロードデータを処理および管理するためのデータクラスです。

## プロパティ

- `itemCode`：項目コードを表す UByte 型のプロパティです。
- `data`：有効なペイロードデータを格納するバイト配列（ByteArray）です。

## メソッド

- `toDataWithHeader()`：このメソッドは、`itemCode` を Byte 型に変換し、それをヘッダーとして `data` と連結した新しいバイト配列を返します。このメソッドは、ペイロードデータを適切な形式に変換して送信または保存する際に使用されます。
