# SesameNotifypayload クラスのドキュメント
```svg
internal class SSM2ResponsePayload(val data: ByteArray) {
    val cmdItCode: UByte = data[0].toUByte()// login
    val cmdOPCode: UByte = data[1].toUByte()// sync
    val cmdResultCode: UByte = data[2].toUByte()// success
    val payload: ByteArray = data.drop(3).toByteArray()
}

```
### 説明

`SesameNotifypayload` は、'Sesame' の通知に関連するペイロード（データの負荷部分）を処理するためのKotlinの内部クラスです。このクラスをインスタンス化する際には、バイト配列（ByteArray）を引数として渡す必要があります。

### プロパティ

- `data`：ByteArray型のインスタンス変数です。この `data` 変数は、クラスのインスタンスを作成する際に提供する必要があります。

- `notifyOpCode`：`data` 配列の最初のバイトの値を取得し、それを `SSM2OpCode.getByValue` 関数に渡して得られた結果を `notifyOpCode` とします。

- `payload`：これは `notifyOpCode` の後に残る `data` 部分です。`data.drop(1)` を使用して、`data` 中の最初の要素を除いたすべての要素で新しい配列を生成します。

### 使用方法

このクラスを使用するには、`SesameNotifypayload` クラスのインスタンスを作成し、そのコンストラクタにバイト配列を提供する必要があります。この配列の最初のバイトは、`notifyOpCode` に渡され、残りの部分は `payload` に格納されます。
