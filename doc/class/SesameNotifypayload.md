## SesameNotifypayload クラス
```
internal class SesameNotifypayload(val data: ByteArray) {
    val notifyOpCode: SSM2OpCode = SSM2OpCode.getByValue(data[0])
    val payload: ByteArray = data.drop(1).toByteArray()
}
```
### 説明

`SesameNotifypayload`は`Sesame`の通知のペイロードを処理するためのKotlinの内部クラスです。このクラスのインスタンス化する際には、バイト配列を渡す必要があります。

### プロパティ

- `data`:ByteArray型のインスタンス変数です。この`data`変数は、クラスのインスタンスを作成する際に提供する必要があります。 

- `notifyOpCode`: 提供された`data`配列の最初のバイトの値を取得し、`SSM2OpCode.getByValue`関数に渡して、その結果を`notifyOpCode`として設定します。

- `payload`: `notifyOpCode`の後にある`data`の残りの部分です。`data.drop(1)`を使用すると、新しい配列が生成され、最初の要素以外のすべての要素が含まれます。

### 使用

このクラスを使用するには、`SesameNotifyPayload`オブジェクトをインスタンス化し、そのコンストラクタにバイト配列を提供する必要があります。