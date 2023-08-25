# SSM3PublishPayload クラスドキュメント
```svg
class SSM3PublishPayload(val data: ByteArray) {
    val cmdItCode = data[0].toUByte()
    val payload: ByteArray = data.drop(1).toByteArray()
}
```
### 説明

`SSM3PublishPayload` は、SSM3プロトコルに関連するペイロード（データの負荷部分）を処理するためのKotlinクラスです。このクラスをインスタンス化する際には、バイト配列（ByteArray）を引数として渡す必要があります。

### プロパティ

- `data`：ByteArray型のインスタンス変数です。この変数の値は、クラスのインスタンスを作成する際に提供する必要があります。

- `cmdItCode`：提供された `data` 配列の最初のバイトを `UByte` （符号なしバイト）に変換した値です。

- `payload`：これは `cmdItCode` の後に残る `data` 部分です。`data.drop(1)` を使用して、`data` 中の最初の要素を除いたすべての要素から新しい配列を生成します。

### 使用方法

このクラスを使用するには、`SSM3PublishPayload` クラスのインスタンスを作成し、そのコンストラクタにバイト配列を提供する必要があります。この配列の最初のバイトは `cmdItCode` に変換され、残りの部分は `payload` に格納されます。
