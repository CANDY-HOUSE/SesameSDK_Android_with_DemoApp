# SesameSDK3.0 for Android          [中文看這裡](README.md)       

## ![CANDY HOUSE](https://jp.candyhouse.co/cdn/shop/files/3_eea4302e-b1ab-435d-8112-f97d85d5eda2.png?v=1682502225&width=18)[CANDY HOUSE 公式サイト](https://jp.candyhouse.co/)

##### Google Play Apk [ダウンロード](https://play.google.com/store/apps/details?id=co.candyhouse.sesame2)
## プロジェクト概要
#### Sesame 5、Sesame 5 Pro、Sesame Bike2、BLE Connector1、Open Sensor、Sesame Touch 1 Pro、Sesame Touch 1、WIFI Module2、Sesame Bot 1、Sesame 3、Sesame 4、Sesame Bike 1に対応しています。
##  テックスタックとツール
- [プログラミング言語 Kotlin](https://kotlinlang.org/)
- [android studio](https://developer.android.com/studio)


========
### 1 .プロジェクト依存
```svg
   implementation project(':sesame-sdk')
```
### 2. manifest.xml で権限を登録
```agsl
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT " />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
```
### 3. アプリケーションの初期化

```agsl
   override fun onCreate() {
        super.onCreate()
           CHBleManager(this)
        }
```
CHBleManager の初期化では、デバイスの Bluetooth の正常性、権限の有効化、Bluetooth の有効化などが判定されます。すべて正常なら Bluetooth スキャンを開始します。
Bluetooth の Service UUID: 0000FD81-0000-1000-8000-00805f9b34fb
```agsl
 bluetoothAdapter.bluetoothLeScanner.startScan(
 mutableListOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID.fromString("0000FD81-0000-1000-8000-00805f9b34fb"))).build()), ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), bleScanner)

```
bleScanner はデバイスをスキャンして chDeviceMap に格納します。


### 4. 新しいデバイスを ScanNewDeviceFG オブジェクトに追加し、Adapter は chDeviceMap をフィルタリングして (it.rssi!=null) コレクションデータをリストに表示します。
```svg
    private var mDeviceList = ArrayList<CHDevices>()
 CHBleManager.delegate = object : CHBleManagerDelegate {
            override fun didDiscoverUnRegisteredCHDevices(devices: List<CHDevices>) {

          //     L.d("devices size",devices.size.toString())
                mDeviceList.clear()
                mDeviceList.addAll(devices.filter { it.rssi != null }
//                    .filter { it.rssi!! > -65 }///註冊列表只顯示距離近的
                )
                mDeviceList.sortBy { it.getDistance() }
                mDeviceList.firstOrNull()?.connect { }
                leaderboard_list.post((leaderboard_list.adapter as GenericAdapter<*>)::notifyDataSetChanged)
            }
        }
    }
```

### 5. デバイスを準備し、device を接続し、onBleDeviceStatusChanged を通じてデバイスの状態を監視
```agsl
            device.connect { }
            doRegisterDevice(device)
            device.delegate = object : CHDeviceStatusDelegate {
                override fun onBleDeviceStatusChanged(device: CHDevices, status: CHDeviceStatus, shadowStatus: CHDeviceStatus?) {
                    if (status == CHDeviceStatus.ReadyToRegister) {
                      doRegisterDevice(device)
                       
                    }
                }
            }
            
           
   fun  doRegisterDevice(device: CHDevices){
       device.register {
       it.onSuccess {
           //  注册成功
       }
       it.onFailure {
          //  注册失败
           }
       }
   }
```
### 6. device.registerは、デバイスがどの製品モデルに所属しているかを判断するためのコールバック登録の成功失敗コマンドです
```svg
                    (device as? CHWifiModule2)?.let {

                    }
                    (device as? CHSesame2)?.let {
                     
                    }
                    (device as? CHSesame5)?.let {
                    
                    }
                    (device as? CHSesameTouchPro)?.let {
                       
                    }

```
- [セサミ5](doc/command/sesame5fun_jp.md):このインスタンスオブジェクトは、Sesame5、Sesame5 pro製品に適用されます。
- [セサミバイク2](doc/command/sesamebike2fun_jp.md) : このインスタンスオブジェクトは、Sesame Bike 2製品に適用されます。
- [セサミWiFiモジュール2](doc/command/sesamewifimodule_jp.md):このインスタンスオブジェクトは、Sesame WiFi Module 2製品に適用されます
- [セサミタッチプロ](doc/command/sesametouchpro_jp.md):このインスタンスオブジェクトは、Sesame BLE Connector1、Sesame Touch 1 Pro、Sesame Touch 1製品に適用されます。
- [セサミオープンセンサー1](doc/command/sesame_open_sensor_jp.md):このインスタンスオブジェクトは、Sesame Open Sensor 1製品に適用され
- [Class オブジェクト](doc/class/allclass_jp.md)
### 循环图
![BleConnect](doc/bleprotocol/BleConnect.svg)
## [プロジェクト構造](./doc/product_structure_ja.md)

## [プロジェクトフレームワーク](./doc/Sesame_framework_ja.md)

## [プロジェクトインターフェース](./doc/APP_instroduce_ja.md)



##  [他の関連説明](./doc/sesame_code_ja.md)



### Android関連の知識
- [Android Ble](https://developer.android.com/guide/topics/connectivity/bluetooth-le?hl=zh-cn)
- [Android Nfc](https://developer.android.com/guide/topics/connectivity/nfc?hl=zh-cn)
- [Android jetpack](https://developer.android.com/jetpack?hl=zh-cn)

### [SDK利用規約](https://jp.candyhouse.co/pages/sesamesdk%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84)

