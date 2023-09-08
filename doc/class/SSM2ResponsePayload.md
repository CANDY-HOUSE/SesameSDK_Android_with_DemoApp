## SSM2ResponsePayload クラス
```svg
internal class SSM2ResponsePayload(val data: ByteArray) {
    val cmdItCode: UByte = data[0].toUByte()// login
    val cmdOPCode: UByte = data[1].toUByte()// sync
    val cmdResultCode: UByte = data[2].toUByte()// success
    val payload: ByteArray = data.drop(3).toByteArray()
}

```
### 説明

`SSM2ResponsePayload`は、SSM2プロトコルの応答ペイロード（payload）を処理するためのKotlinの内部クラスです。このクラスをインスタンス化する際には、バイト配列を引数として渡す必要があります.

### プロパティ

- `data`:バイト配列（ByteArray）型のインスタンス変数です。この`data`変数は、クラスのインスタンスを作成する際には、提供する必要があります。

- `cmdItCode`: 提供された`data`配列の最初の要素をUByte型（符号なしバイト）に変換します。ログイン操作を示します。

- `cmdOPCode`: 提供された`data`配列の2番目の要素をUByte型に変換します。同期操作を示します。

- `cmdResultCode`: 提供された`data`配列の3番目の要素をUByte型に変換します。操作が成功したかどうかを示します。

- `payload`: `cmdResultCode`の後に`data`の残りの部分です.