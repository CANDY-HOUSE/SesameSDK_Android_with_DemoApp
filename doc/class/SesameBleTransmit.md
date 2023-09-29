# SesameBleTransmit クラス

```svg

internal class SesameBleTransmit(var type: DeviceSegmentType, var input: ByteArray) {
    var isStart = 1
    internal fun getChunk(): ByteArray?
}

```

`SesameBleTransmit` は内部のクラスです。

## プロパティ

- `type`：`DeviceSegmentType`の列挙型の値です。転送するデータのタイプを表します。
- `input`：バイト配列です。転送する必要があるデータを表します。
- `isStart`：データ転送の開始を示すために使用される整数値です。初期値は1であり、転送が開始されると0に設定され、転送が終了すると-1に設定されます。

## メソッド

- `getChunk(): ByteArray?`：バイト配列を返し、データブロックを表します。`isStart`が-1の場合、データの転送が完了したことを示し、`null`を返します。また、`input`が19以下の場合、すべてのデータを一度に転送し、`isStart`を-1に設定します。`input`が19より大きい場合、19バイトずつデータを転送し、すべてのデータが転送されるまで繰り返します。
