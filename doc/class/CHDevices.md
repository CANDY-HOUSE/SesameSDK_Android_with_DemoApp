# CHDevices インターフェース

```svg

interface CHDevices {

    var mechStatus: CHSesameProtocolMechStatus?

    var deviceTimestamp:Long?

    var loginTimestamp:Long?

    var delegate: CHDeviceStatusDelegate?

    var deviceStatus: CHDeviceStatus

    var deviceShadowStatus: CHDeviceStatus?

    var rssi: Int?

    var deviceId: UUID?

    var isRegistered: Boolean

    var productModel: CHProductModel

    fun connect(result: CHResult<CHEmpty>)

    fun disconnect(result: CHResult<CHEmpty>)

    fun getKey(): CHDevice 
    fun dropKey(result: CHResult<CHEmpty>)

    fun getVersionTag(result: CHResult<String>)
                    
    fun register(result: CHResult<CHEmpty>)
                        
    fun reset(result: CHResult<CHEmpty>)
                            
    fun updateFirmware(onResponse: CHResult<BluetoothDevice>)

    fun createGuestKey(keyName: String, result: CHResult<CHDevice>) 

    fun getGuestKeys(result: CHResult<Array<CHGuestKeyCut>>) 

    fun removeGuestKey(guestKeyId: String, result: CHResult<CHEmpty>)

    fun updateGuestKey(guestKeyId: String, name: String, result: CHResult<CHEmpty>) 

    fun setHistoryTag(tag: ByteArray, result: CHResult<CHEmpty>) 

    fun getHistoryTag(): ByteArray? 

    fun getTimeSignature(): String 

}
```


`CHDevices` デバイスに関連するインターフェースで、デバイスに対して、接続、切断、バージョン取得、登録、リセット、ファームウェアの更新などの基本的な操作ができます。
## プロパティ

- `mechStatus: CHSesameProtocolMechStatus?` - デバイスの機械状態
- `deviceTimestamp: Long?` - デバイスのタイムスタンプ
- `loginTimestamp: Long?` - ログインのタイムスタンプ
- `delegate: CHDeviceStatusDelegate?` - デバイス状態のDelegate
- `deviceStatus: CHDeviceStatus` - デバイスの状態
- `deviceShadowStatus: CHDeviceStatus?` - デバイスシャドウの状態
- `rssi: Int?` - 受信号強度の指示子
- `deviceId: UUID?` - デバイスの唯一の識別子
- `isRegistered: Boolean` - 登録しているかどうか
- `productModel: CHProductModel` - デバイスの製品モデル

## メソッド

- `fun connect(result: CHResult<CHEmpty>)` - デバイスを接続します。
- `fun disconnect(result: CHResult<CHEmpty>)` - デバイスの接続を切断します。
- `fun getKey(): CHDevice` - デバイスのキーデータを取得します。
- `fun dropKey(result: CHResult<CHEmpty>)` - デバイスのキーデータを破棄します。
- `fun getVersionTag(result: CHResult<String>)` - デバイスのバージョンラベルを取得します。
- `fun register(result: CHResult<CHEmpty>)` - デバイスを登録します。
- `fun reset(result: CHResult<CHEmpty>)` - デバイスをリセットします。
- `fun updateFirmware(onResponse: CHResult<BluetoothDevice>)` - デバイスのファームウェアを更新します。
- `fun createGuestKey(keyName: String, result: CHResult<CHDevice>)` - 訪問者のキーデータを作成します。
- `fun getGuestKeys(result: CHResult<Array<CHGuestKeyCut>>)` - 訪問者のキーデータを取得します。
- `fun removeGuestKey(guestKeyId: String, result: CHResult<CHEmpty>)` - 訪問者のキーデータを削除します。
- `fun updateGuestKey(guestKeyId: String, name: String, result: CHResult<CHEmpty>)` - 訪問者のキーデータを更新します。
- `fun setHistoryTag(tag: ByteArray, result: CHResult<CHEmpty>)` - 歴史のタグを設定します。
- `fun getHistoryTag(): ByteArray?` - 歴史のタグを取得します。
- `fun getTimeSignature(): String` - 時間の署名を取得します。

