# CHWifiModule2Device クラスのドキュメント
```svg
internal class CHWifiModule2Device : CHSesameOS3(), CHWifiModule2, CHDeviceUtil {

    // Properties
    override var ssm2KeysMap: MutableMap<String, String> = mutableMapOf()
    override var mechSetting: CHWifiModule2MechSettings? = CHWifiModule2MechSettings(null, null)
      


    override var advertisement: CHadv? = null
      

    override fun connect(result: CHResult<CHEmpty>) {
      
    }

    override fun register(result: CHResult<CHEmpty>) {
       
    }

    override fun login(token: String?) {
    }

    override fun scanWifiSSID(result: CHResult<CHEmpty>) {
    }

    override fun setWifiSSID(ssid: String, result: CHResult<CHEmpty>) {
    }

    override fun setWifiPassword(password: String, result: CHResult<CHEmpty>) {
    }

    override fun connectWifi(result: CHResult<CHEmpty>) {
    }

    override fun insertSesames(sesame: CHDevices, result: CHResult<CHEmpty>) {
    }

    override fun removeSesame(sesameKeyTag: String, result: CHResult<CHEmpty>) {
    }

    override fun getVersionTag(result: CHResult<String>) {
    }

    override fun reset(result: CHResult<CHEmpty>) {
    }

    override fun updateFirmware(onResponse: CHResult<BluetoothDevice>) {
    }

  
    val mBluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback()
  
    private fun onGattWM2Publish(receivePayload: SSM3PublishPayload) {
    }
}

```
`CHWifiModule2Device` クラスは `CHSesameOS3` クラスを継承し、`CHWifiModule2` および `CHDeviceUtil` インターフェースを実装したクラスです。これは主に Wi-Fi モジュール2 デバイスに関連する操作を処理するためのものです。

## 属性

- `ssm2KeysMap`：キーバリューのペアを格納する `MutableMap` です。
- `mechSetting`：`CHWifiModule2MechSettings` オブジェクトで、機械設定を設定します。
- `advertisement`：`CHadv` オブジェクトで、広告を表します。

## メソッド

- `connect(result: CHResult<CHEmpty>)`：デバイスに接続するメソッドです。
- `register(result: CHResult<CHEmpty>)`：デバイスを登録するメソッドです。
- `login(token: String?)`：デバイスにログインするメソッドです。
- `scanWifiSSID(result: CHResult<CHEmpty>)`：Wi-Fi SSID をスキャンするメソッドです。
- `setWifiSSID(ssid: String, result: CHResult<CHEmpty>)`：Wi-Fi SSID を設定するメソッドです。
- `setWifiPassword(password: String, result: CHResult<CHEmpty>)`：Wi-Fi パスワードを設定するメソッドです。
- `connectWifi(result: CHResult<CHEmpty>)`：Wi-Fi に接続するメソッドです。
- `insertSesames(sesame: CHDevices, result: CHResult<CHEmpty>)`：Sesame デバイスを挿入するメソッドです。
- `removeSesame(sesameKeyTag: String, result: CHResult<CHEmpty>)`：Sesame デバイスを削除するメソッドです。
- `getVersionTag(result: CHResult<String>)`：バージョンタグを取得するメソッドです。
- `reset(result: CHResult<CHEmpty>)`：デバイスをリセットするメソッドです。
- `updateFirmware(onResponse: CHResult<BluetoothDevice>)`：ファームウェアを更新するメソッドです。
- `mBluetoothGattCallback`：BluetoothGattCallback オブジェクトです。Bluetooth GATT コールバックを処理します。
- `onGattWM2Publish(receivePayload: SSM3PublishPayload)`：GATT WM2 のパブリッシュを処理するメソッドです。

![CHWifiModule2Device](CHWifiModule2Device.svg)
