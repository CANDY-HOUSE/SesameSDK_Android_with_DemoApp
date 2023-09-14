# CHSesameConnector インターフェース
```svg


interface CHSesameConnector : CHDevices {
    var ssm2KeysMap: MutableMap<String, ByteArray>
    fun insertSesame(sesame: CHDevices, result: CHResult<CHEmpty>)
    fun removeSesame(tag: String, result: CHResult<CHEmpty>)
}
```
`CHSesameConnector` はデバイスコネクタのインターフェースで、`CHDevices`を継承しています。Sesameデバイスを管理および操作します。

## プロパティ

- `ssm2KeysMap: MutableMap<String, ByteArray>` - Sesame2デバイスのキーのマッピングを保存します。

## メソッド

- `fun insertSesame(sesame: CHDevices, result: CHResult<CHEmpty>)` - Sesameデバイスを挿入し、操作結果を返します。
- `fun removeSesame(tag: String, result: CHResult<CHEmpty>)` - 指定されたラベルに基づいてSesameデバイスを削除し、操作結果を返します。

