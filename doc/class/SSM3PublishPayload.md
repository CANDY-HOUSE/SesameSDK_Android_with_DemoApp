## SSM3PublishPayload クラス
```svg
class SSM3PublishPayload(val data: ByteArray) {
    val cmdItCode = data[0].toUByte()
    val payload: ByteArray = data.drop(1).toByteArray()
}
```
### 説明

`SSM3PublishPayload` は、SSM3プロトコルに関連するペイロード（データ）を処理するためのKotlinクラスです。このクラスをインスタンス化する際には、バイト配列（ByteArray）を引数として渡す必要があります。

### プロパティ

- `data`：ByteArray型のインスタンス変数です。この変数の値は、クラスのインスタンスを作成する際に提供する必要があります。

- `cmdItCode`：提供された`data`配列の最初のバイトをUByte（符号なしバイト）に変換します。

- `payload`：`cmdItCode`の後に残る`data`の部分です。`data.drop(1)`を使用して、最初の要素を除いたすべての要素が含まれる新しい配列を生成します。

### 使用

このクラスを使用するには、`SSM3PublishPayload`クラスのインスタンスを作成し、コンストラクタにバイト配列を渡す必要があります。この配列の最初のバイトは`cmdItCode`に変換されます。