SesameOs3 Ble 接続プロセス
========
### 1. manifest.xml でAndroid権限を設定しましょう
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
### 2. application 初期化 
```agsl
   override fun onCreate() {
        super.onCreate()
           CHBleManager(this)
        }
```
CHBleManagerの初期化は、端末のBluetoothが正常に動作しているか、端末から権限をもらっているか、Bluetoothが起動しているかを判断します。すべてが正常に動作している場合、Bluetoothスキャンが始まります。
Bluetooth ServiceUuid:0000FD81-0000-1000-8000-00805f9b34fb
```agsl
 bluetoothAdapter.bluetoothLeScanner.startScan(
 mutableListOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID.fromString("0000FD81-0000-1000-8000-00805f9b34fb"))).build()), ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), bleScanner)

```
bleScannerがスキャンしたデバイスをCHDeviceMapに入れる。

### 3. 新規デバイスはScanNewDeviceFGオブジェクトに追加され、AdapterはCHDeviceMapからフィルタリングされた(it.rssi!=null)データをリストで表示します。
```svg
    private var mDeviceList = ArrayList<CHDevices>()
 CHBleManager.delegate = object : CHBleManagerDelegate {
            override fun didDiscoverUnRegisteredCHDevices(devices: List<CHDevices>) {

          //     L.d("devices size",devices.size.toString())
                mDeviceList.clear()
                mDeviceList.addAll(devices.filter { it.rssi != null }
//                    .filter { it.rssi!! > -65 }///登録リストが近くの項目のみを表示する註冊列表只顯示距離近的
                mDeviceList.sortBy { it.getDistance() }
                mDeviceList.firstOrNull()?.connect { }
                leaderboard_list.post((leaderboard_list.adapter as GenericAdapter<*>)::notifyDataSetChanged)
            }
        }
    }
```
### 4. デバイスとの接続手順は、connectを実行し、onBleDeviceStatusChangedでデバイスの状態を監視すること。
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
           //  登録成功
       }
       it.onFailure {
          //  登録失敗
           }
       }
   }
```
### 5. device.register登録コマンドの成功や失敗のコールバックによって、所属製品のdevice modelを判断できます。
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
- [Sesame 5](../command/sesame5fun.md):このインスタンスオブジェクトは、Sesame5 、Sesame5 pro 製品に適用される。
- [Sesame Bike 2](../command/sesamebike2fun.md) :このインスタンスオブジェクトは、Sesame Bike 2 製品に適用される。
- [Sesame WiFi Module 2](../command/sesamewifimodule.md):このインスタンスオブジェクトは、Sesame WiFi Module 2 製品に適用される。
- [Sesame touch pro](../command/sesametouchpro.md):このインスタンスオブジェクトは、Sesame BLE Connector1、 Sesame Touch 1 Pro 、  Sesame Touch 1 製品に適用される。
- [Sesame Open Sensor 1](../command/sesame_open_sensor.md):このインスタンスオブジェクトは、Sesame Open Sensor 1 製品に適用される。

### フローチャート
![BleConnect](BleConnect.svg)

