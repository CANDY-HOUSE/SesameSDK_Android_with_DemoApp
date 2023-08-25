# CHDevices インターフェースのドキュメント

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
`CHDevices` は、デバイスに関連する操作を含むインターフェースです。これには接続、切断、バージョン取得、登録、リセット、ファームウェアの更新などが含まれます。

## 属性

- `mechStatus: CHSesameProtocolMechStatus?` - デバイスのメカニズムの状態
- `deviceTimestamp: Long?` - デバイスのタイムスタンプ
- `loginTimestamp: Long?` - ログインのタイムスタンプ
- `delegate: CHDeviceStatusDelegate?` - デバイスの状態のデリゲート
- `deviceStatus: CHDeviceStatus` - デバイスの状態
- `deviceShadowStatus: CHDeviceStatus?` - デバイスのシャドウ状態
- `rssi: Int?` - 受信信号強度指示器
- `deviceId: UUID?` - デバイスの一意の識別子
- `isRegistered: Boolean` - デバイスが登録済みかどうか
- `productModel: CHProductModel` - デバイスの製品モデル

## メソッド

- `fun connect(result: CHResult<CHEmpty>)` - デバイスに接続する
- `fun disconnect(result: CHResult<CHEmpty>)` - デバイスの接続を切断する
- `fun getKey(): CHDevice` - デバイスのキーを取得する
- `fun dropKey(result: CHResult<CHEmpty>)` - デバイスのキーを破棄する
- `fun getVersionTag(result: CHResult<String>)` - デバイスのバージョンタグを取得する
- `fun register(result: CHResult<CHEmpty>)` - デバイスを登録する
- `fun reset(result: CHResult<CHEmpty>)` - デバイスをリセットする
- `fun updateFirmware(onResponse: CHResult<BluetoothDevice>)` - デバイスのファームウェアを更新する
- `fun createGuestKey(keyName: String, result: CHResult<CHDevice>)` - ゲストキーを作成する
- `fun getGuestKeys(result: CHResult<Array<CHGuestKeyCut>>)` - ゲストキーを取得する
- `fun removeGuestKey(guestKeyId: String, result: CHResult<CHEmpty>)` - ゲストキーを削除する
- `fun updateGuestKey(guestKeyId: String, name: String, result: CHResult<CHEmpty>)` - ゲストキーを更新する
- `fun setHistoryTag(tag: ByteArray, result: CHResult<CHEmpty>)` - 履歴タグを設定する
- `fun getHistoryTag(): ByteArray?` - 履歴タグを取得する
- `fun getTimeSignature(): String` - 時間の署名を取得する
