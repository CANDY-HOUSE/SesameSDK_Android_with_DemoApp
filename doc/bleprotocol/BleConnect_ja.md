SesameOs3 BLE 接続フロー
========
### 1. manifest.xml で権限を登録
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
### 2. アプリケーションの初期化

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


### 3. 新しいデバイスを ScanNewDeviceFG オブジェクトに追加し、Adapter は chDeviceMap をフィルタリングして (it.rssi!=null) コレクションデータをリストに表示します。
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

### 4. デバイスを準備し、device を接続し、onBleDeviceStatusChanged を通じてデバイスの状態を監視
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
### 5. device.registerは、デバイスがどの製品モデルに所属しているかを判断するためのコールバック登録の成功失敗コマンドです
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
- [セサミ5](../command/sesame5fun_jp.md):このインスタンスオブジェクトは、Sesame5、Sesame5 pro製品に適用されます。
- [セサミバイク2](../command/sesamebike2fun_jp.md) : このインスタンスオブジェクトは、Sesame Bike 2製品に適用されます。
- [セサミWiFiモジュール2](../command/sesamewifimodule_jp.md):このインスタンスオブジェクトは、Sesame WiFi Module 2製品に適用されます
- [セサミタッチプロ](../command/sesametouchpro_jp.md):このインスタンスオブジェクトは、Sesame BLE Connector1、Sesame Touch 1 Pro、Sesame Touch 1製品に適用されます。
- [セサミオープンセンサー1](../command/sesame_open_sensor_jp.md):このインスタンスオブジェクトは、Sesame Open Sensor 1製品に適用され
 
### シーケンス図
![BleConnect](BleConnect.svg)