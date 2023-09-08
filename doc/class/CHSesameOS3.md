

# CHSesameOS3 クラス
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
`CHSesameOS3` は公開された内部クラスです。 `CHBaseDevice`を継承し、`CHSesameOS3Publish` インターフェースを実装しています。

## メンバー変数

- `var cipher: SesameOS3BleCipher?`: 暗号化と復号化に使うインスタントです。

- `var cmdCallBack: MutableMap<UByte, SesameOS3ResponseCallback>`: コールバック関数を格納するためのマッピングです。

- `var semaphore: Semaphore`: 並行制御に使用するセマフォです。

## メソッド

- `open fun connect(result: CHResult<CHEmpty>)`: デバイスに接続する方法です。

- `private val mBluetoothGattCallback: BluetoothGattCallback`: Bluetooth GATT のコールバック関数です。

- `fun transmit()`: データを転送します。

- `fun sendCommand(payload: SesameOS3Payload, isEncryt: DeviceSegmentType = DeviceSegmentType.cipher, onResponse: SesameOS3ResponseCallback)`: コマンドを送信します。

- `open fun getVersionTag(result: CHResult<String>)`: バージョンのタグを取得します。

- `open fun reset(result: CHResult<CHEmpty>)`: デバイスをリセットします。

- `open fun updateFirmware(onResponse: CHResult<BluetoothDevice>)`: ファームウェアを更新します。

- `fun parceADV(value: CHadv?)`: アドバタイジングのデータを解析します。

- `override fun onGattSesamePublish(receivePayload: SSM3PublishPayload)`: GATTが発行するメッセージを処理します。

以上は`CHSesameOS3`クラスの基本的な説明です。このクラスはSesame OS3デバイスに対して完全な操作と管理の方法を提供します。
