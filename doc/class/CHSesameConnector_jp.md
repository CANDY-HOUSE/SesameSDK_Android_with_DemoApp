# CHSesameConnector インターフェースのドキュメント

```svg


interface CHSesameConnector : CHDevices {
    var ssm2KeysMap: MutableMap<String, ByteArray>
    fun insertSesame(sesame: CHDevices, result: CHResult<CHEmpty>)
    fun removeSesame(tag: String, result: CHResult<CHEmpty>)
}
```

`CHSesameConnector` は、デバイス接続コネクタのインターフェースであり、`CHDevices` インターフェースを継承しています。Sesame デバイスの管理と操作を担当します。

## 属性

- `ssm2KeysMap: MutableMap<String, ByteArray>` - Sesame2 デバイスのキーを格納するマップです。

## メソッド

- `fun insertSesame(sesame: CHDevices, result: CHResult<CHEmpty>)` - Sesame デバイスを挿入し、操作結果を返します。
- `fun removeSesame(tag: String, result: CHResult<CHEmpty>)` - 指定されたタグに基づいて Sesame デバイスを削除し、操作結果を返します。
