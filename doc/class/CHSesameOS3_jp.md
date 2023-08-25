# CHSesameOS3 クラスのドキュメント
```svg

internal open class CHSesameOS3 : CHBaseDevice(), CHSesameOS3Publish {
    var cipher: SesameOS3BleCipher? = null

    var cmdCallBack: MutableMap<UByte, SesameOS3ResponseCallback> = mutableMapOf()

    var semaphore: Semaphore = Semaphore(1)

    open fun connect(result: CHResult<CHEmpty>) 

    private val mBluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() 

    fun transmit() 

    fun sendCommand(payload: SesameOS3Payload, isEncryt: DeviceSegmentType = DeviceSegmentType.cipher, onResponse: SesameOS3ResponseCallback) 

    open fun getVersionTag(result: CHResult<String>)

    open fun reset(result: CHResult<CHEmpty>)

    open fun updateFirmware(onResponse: CHResult<BluetoothDevice>)

    fun parceADV(value: CHadv?)

    override fun onGattSesamePublish(receivePayload: SSM3PublishPayload)
}

```
`CHSesameOS3` は、`CHBaseDevice` を継承し、`CHSesameOS3Publish` インターフェースを実装した開放的な内部クラスです。

## メンバー変数

- `var cipher: SesameOS3BleCipher?`：暗号化と復号化に使用されるインスタンス。

- `var cmdCallBack: MutableMap<UByte, SesameOS3ResponseCallback>`：コールバック関数を保持するためのマップ。

- `var semaphore: Semaphore`：並行処理を制御するためのセマフォ。

## メソッド

- `open fun connect(result: CHResult<CHEmpty>)`：デバイスに接続するメソッド。

- `private val mBluetoothGattCallback: BluetoothGattCallback`：Bluetooth GATT コールバック関数。

- `fun transmit()`：データの送信。

- `fun sendCommand(payload: SesameOS3Payload, isEncryt: DeviceSegmentType = DeviceSegmentType.cipher, onResponse: SesameOS3ResponseCallback)`：コマンドの送信。

- `open fun getVersionTag(result: CHResult<String>)`：バージョンタグの取得。

- `open fun reset(result: CHResult<CHEmpty>)`：デバイスのリセット。

- `open fun updateFirmware(onResponse: CHResult<BluetoothDevice>)`：ファームウェアの更新。

- `fun parceADV(value: CHadv?)`：広告データの解析。

- `override fun onGattSesamePublish(receivePayload: SSM3PublishPayload)`：GATT パブリッシュメッセージの処理。

上記は `CHSesameOS3` クラスの基本的な説明であり、このクラスは Sesame OS3 デバイスに対して完全な操作と管理メソッドを提供します。
