# CHSesameBike2Device クラスのドキュメント

```svg
internal class CHSesameBike2Device : CHSesameOS3(), CHSesameBike2, CHDeviceUtil {

    override var advertisement: CHadv? = null
     
    override fun unlock(tag: ByteArray?, result: CHResult<CHEmpty>) 
    override fun register(result: CHResult<CHEmpty>) 

    override fun login(token: String?) 

    override fun onGattSesamePublish(receivePayload: SSM3PublishPayload)
}

```

`CHSesameBike2Device` クラスは `CHSesameOS3` クラスを継承し、`CHSesameBike2` および `CHDeviceUtil` インターフェースを実装したクラスです。これは主に自転車のロックデバイスを管理および制御するためのものです。

## プロパティ

- `advertisement`：デバイスの広告データ。

## メソッド

- `unlock(tag: ByteArray?, result: CHResult<CHEmpty>)`：デバイスのロックを解除するメソッド。バイト配列のタグと結果パラメータを受け取り、デバイスにアンロックコマンドを送信します。

- `register(result: CHResult<CHEmpty>)`：デバイスを登録するメソッド。結果パラメータを受け取り、デバイスに登録コマンドを送信します。登録中にデバイス状態とメカニズム状態の変更も処理します。

- `login(token: String?)`：デバイスにログインするメソッド。トークンパラメータを受け取り、デバイスにログインコマンドを送信します。ログイン中にデバイス状態の変更も処理します。

- `onGattSesamePublish(receivePayload: SSM3PublishPayload)`：GATT Sesame パブリッシュを処理するメソッド。パブリッシュのペイロードを受け取り、デバイス状態とメカニズム状態を更新します。

以上が `CHSesameBike2Device` クラスの基本的な説明です。これは主に自転車のロックデバイスに対して操作と管理のメソッドを提供します。
