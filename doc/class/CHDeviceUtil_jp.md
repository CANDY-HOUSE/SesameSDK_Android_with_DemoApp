# CHDeviceUtil インターフェースドキュメント
```svg
internal interface CHDeviceUtil {
    var advertisement: CHadv?
    var sesame2KeyData: CHDevice?
    fun goIOT() {}
    fun login(token: String? = null)
}
```
### 説明

`CHDeviceUtil` は、デバイス関連の操作を定義するための内部インターフェースです。

### プロパティ

- `advertisement`：`CHadv` 型の変数で、広告に関連するデータです。広告がない場合、`null` の可能性があります。

- `sesame2KeyData`：`CHDevice` インスタンス変数で、デバイス（おそらく "鍵" を指す）に関連するデータが格納されます。

### メソッド

- `goIOT` メソッド：このメソッドは、IoT（Internet of Things）に関連するサービスを購読するためのものです。このメソッドは、インターフェース内でデフォルトの空実装が提供されていますが、具体的な実装クラスで上書きすることができます。

- `login` メソッド：このメソッドは、ログイン操作に使用されます。匿名引数 `token` のデフォルト値は `null` です。

### 使用方法

このインターフェースを使用するには、`CHDeviceUtil` インターフェースを実装したクラスを作成し、そのクラスをインスタンス化します。そのクラス内では、必要に応じて
