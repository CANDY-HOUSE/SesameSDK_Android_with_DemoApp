
## SSM3ResponsePayload クラス
```svg
internal class SSM3ResponsePayload(val data: ByteArray) {
    val cmdItCode: UByte = data[0].toUByte()
    val cmdResultCode: UByte = data[1].toUByte()
    val payload: ByteArray = data.drop(2).toByteArray()
}
```


### 説明

`SSM3ResponsePayload` は、SSM3プロトコルの応答ペイロード（payload）を処理するためのKotlinの内部クラスです。このクラスをインスタンス化する際には、バイト配列を引数として渡す必要があります.

### プロパティ

- `data`: バイト配列（ByteArray）型のインスタンス変数です。この`data`変数は、クラスのインスタンスを作成する際には、提供する必要があります。

- `cmdItCode`: 提供された`data`配列の最初の要素をUByte型（符号なしバイト）に変換します。

- `cmdResultCode`: 提供された`data`配列の2番目の要素をUByte型に変換します。操作の結果コードを示します。

- `payload`: `cmdResultCode`の後に`data`の残りの部分です.这是 `cmdResultCode` 之后的 `data` 的剩余部分。`data.drop(2)` を使用して、最初の2つの要素を除いた、`data` のすべての要素を含む新しい配列を生成します。

### 使用

要使用这个类，你需要实例化一个 `