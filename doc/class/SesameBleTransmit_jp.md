# SesameBleTransmit クラスのドキュメント

```svg

internal class SesameBleTransmit(var type: DeviceSegmentType, var input: ByteArray) {
    var isStart = 1
    internal fun getChunk(): ByteArray?
}

```
`SesameBleTransmit` は、内部クラスです。

## プロパティ

- `type`：`DeviceSegmentType` 列挙値の変数で、データの種類を示します。
- `input`：バイト配列型の変数で、送信するデータを表します。
- `isStart`：整数型の変数で、データ送信の開始を示すために使用されます。初期値は1で、送信が開始されると0に、送信が終了すると-1に設定されます。

## メソッド

- `getChunk(): ByteArray?`：このメソッドは、データのチャンク（一部）を表すバイト配列を返します。`isStart` が-1の場合、データがすべて送信されていることを示します。この場合、`null` を返します。`input` のサイズが19以下の場合、すべてのデータが一度に送信され、`isStart` が-1に設定されます。`input` のサイズが19より大きい場合、19バイトのデータが一度に送信され、すべてのデータが送信されるまで繰り返し送信されます。
