


# CHSesameBike2Device クラス
```svg
internal class CHSesameBike2Device : CHSesameOS3(), CHSesameBike2, CHDeviceUtil {

    override var advertisement: CHadv? = null
     
    override fun unlock(tag: ByteArray?, result: CHResult<CHEmpty>) 
    override fun register(result: CHResult<CHEmpty>) 

    override fun login(token: String?) 

    override fun onGattSesamePublish(receivePayload: SSM3PublishPayload)
}

```

`CHSesameBike2Device`クラスは、`CHSesameOS3`クラスを継承し、`CHSesameBike2`と`CHDeviceUtil`インタフェースを実装しています。主に自転車のロックの管理と制御に使用されます。

## プロパティ

- `advertisement`：デバイスのアドバタイジングデータ

## メソッド

- `unlock(tag: ByteArray?, result: CHResult<CHEmpty>)`：デバイスのロックを解除する関数です。バイト配列のタグと結果のパラメータを受取り、デバイスに対してロック解除のコマンドを送信します。
- `register(result: CHResult<CHEmpty>)`：デバイスを登録する関数です。結果のパラメータを受取り、デバイスに登録コマンドを送信します。登録中には、デバイスの状態や機械の状態の変更を処理します。
- `login(token: String?)`：デバイスをログインする関数です。トークンのパラメータを受取り、デバイスにログインコマンドを送信します。ログイン中には、デバイスの状態の変更を処理します。
- `onGattSesamePublish(receivePayload: SSM3PublishPayload)`：GATT Sesameの通知を処理する関数です。送信するデータのパラメータを受取り、デバイスと機械の状態を更新します。


![CHSesameBike2Device](CHSesameBike2Device.svg)
