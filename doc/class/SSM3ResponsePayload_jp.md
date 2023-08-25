# SSM3ResponsePayload クラスのドキュメント
```svg
internal class SSM3ResponsePayload(val data: ByteArray) {
    val cmdItCode: UByte = data[0].toUByte()
    val cmdResultCode: UByte = data[1].toUByte()
    val payload: ByteArray = data.drop(2).toByteArray()
}
```
### 説明

`SSM3ResponsePayload` は、SSM3プロトコルの応答ペイロード（データの負荷部分）を処理するためのKotlinの内部クラスです。このクラスをインスタンス化する際には、バイト配列（ByteArray）を引数として渡す必要があります。

### プロパティ

- `data`：ByteArray型のインスタンス変数です。この `data` 変数は、クラスのインスタンスを作成する際に提供する必要があります。

- `cmdItCode`：提供された `data` 配列の最初のバイトを `UByte` （符号なしバイト）に変換した値です。

- `cmdResultCode`：提供された `data` 配列の2番目のバイトを `UByte` に変換した値です。通常、これは操作の結果コードを表します。

- `payload`：これは `cmdResultCode` の後に残る `data` 部分です。`data.drop(2)` を使用して、`data` 中の最初の2つの要素を除いたすべての要素で新しい配列を生成します。

### 使用方法

このクラスを使用するには、`SSM3ResponsePayload` クラスのインスタンスを作成し、そのコンストラクタにバイト配列を提供する必要があります。この配列の最初のバイトは `cmdItCode` に、2番目のバイトは `cmdResultCode` に渡され、残りの部分は `payload` に格納されます。
