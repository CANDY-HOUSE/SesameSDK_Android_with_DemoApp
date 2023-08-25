SesameOs3 Ble 连接流程
========
### 1. manifest.xml 注册权限
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
### 2. application 初始化 
```agsl
   override fun onCreate() {
        super.onCreate()
           CHBleManager(this)
        }
```
CHBleManager初始化会判断设备蓝牙是否正常、权限开启、蓝牙是否启动。一切正常开启蓝牙扫描
蓝牙ServiceUuid:0000FD81-0000-1000-8000-00805f9b34fb
```agsl
 bluetoothAdapter.bluetoothLeScanner.startScan(
 mutableListOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID.fromString("0000FD81-0000-1000-8000-00805f9b34fb"))).build()), ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), bleScanner)

```
bleScanner 会将扫描到设备放入chDeviceMap 中

### 3. 添加新设备在ScanNewDeviceFG对象中,Adapter会拿到chDeviceMap过滤 (it.rssi!=null) 集合数据显示于列表中
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
### 4. 准备连接设备device 执行conect，onBleDeviceStatusChanged 监听device状态
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
### 5. device.register回调注册成功失败指令，判断device 归属产品 model 
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
- [Sesame 5](../command/sesame5fun.md):该实例对象应用于Sesame5 、Sesame5 pro 产品
- [Sesame Bike 2](../command/sesamebike2fun.md) : 该实例对象应用于 Sesame Bike 2 产品
- [Sesame WiFi Module 2](../command/sesamewifimodule.md):该实例对象应用于 Sesame WiFi Module 2 产品
- [Sesame touch pro](../command/sesametouchpro.md):该实例对象应用于 Sesame BLE Connector1、 Sesame Touch 1 Pro 、  Sesame Touch 1 产品
- [Sesame Open Sensor 1](../command/sesame_open_sensor.md):该实例对象应用于  Sesame Open Sensor 1 产品

### 循环图
![BleConnect](BleConnect.svg)

