## CHDeviceUtil インターフェース
```svg
internal interface CHDeviceUtil {
    var advertisement: CHadv?
    var sesame2KeyData: CHDevice?
    fun goIOT() {}
    fun login(token: String? = null)
}
```



### 説明

`CHDeviceUtil`は内部インターフェースで、デバイス関連の操作を定義するためのメソッドとプロパティを提供します。 

### プロパティ

- `advertisement`: `CHadv`型で、アドバタイジング関連のデータです。アドバタイジングがない場合は「null」になる可能性があります。

- `sesame2KeyData`: `CHDevice` のインスタンス変数で、デバイス（「鍵」を指す場合がある）に関連するデータを保存します。

### メソッド

- `goIOT` メソッド：IoT（Internet of Things）関連のサービスを購読するためのものです。インターフェース内で既にデフォルトの空実装が提供されている上に、具体的な実装クラスで上書きすることができます。

- `login` メソッド：ログイン操作に使用され、匿名パラメータ`token` のデフォルト値は`null`です。

### 使用

このインターフェースを使用するために、`CHDeviceUtil`インターフェースを実装したクラスを作成する上に、クラスのインスタンスを作成します。そのクラス内で、実際の要求に応じてメソッドを実装することが可能です。