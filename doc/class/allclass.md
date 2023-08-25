## enum 列表
- [SSM2OpCode](SSM2OpCode.md) （SSM2操作码）
- [SesameResultCode](SesameResultCode.md) （Sesame结果码）
- [SesameItemCode](SesameItemCode.md) （Sesame项目码）
- [Sesame2HistoryTypeEnum](Sesame2HistoryTypeEnum.md) （Sesame2历史类型枚举）
- [DeviceSegmentType](DeviceSegmentType.md) （设备段类型）
- [CHDeviceStatus](CHDeviceStatus.md) （设备状态）
- [CHDeviceLoginStatus](CHDeviceLoginStatus.md) （CH设备登录状态）
- [WM2ActionCode](WM2ActionCode.md) （WM2操作码）

## class 列表
- [SSM3PublishPayload](SSM3PublishPayload.md) （SSM3发布负载）
- [SesameNotifypayload](SesameNotifypayload.md) （Sesame通知负载）
- [SSM2ResponsePayload](SSM2ResponsePayload.md) （SSM2响应负载）
- [SSM3ResponsePayload](SSM3ResponsePayload.md) （SSM3响应负载）
- [CHDeviceUtil](CHDeviceUtil.md) （设备工具）
- [CHBaseDevice](CHBaseDevice.md) （基础设备）
- [CHadv](CHadv.md) （对 ScanResult 处理）
- [SesameBleReceiver](SesameBleReceiver.md) （Sesame蓝牙接收器）
- [SesameBleTransmit](SesameBleTransmit.md) （Sesame蓝牙发射器）
- [CHSesameBike2MechStatus](CHSesameBike2MechStatus.md) （SesameBike2机械状态）
- [SesameOS3BleCipher](SesameOS3BleCipher.md) （SesameOS3蓝牙密码）
- [CHSesameOS3](CHSesameOS3.md) （SesameOS3）
- [CHSesame5Device](CHSesame5Device.md) （Sesame5设备）
- [CHSesameBike2Device](CHSesameBike2Device.md) （SesameBike2设备）
- [CHSesameTouchProDevice](CHSesameTouchProDevice.md) （SesameTouchPro设备）
- [CHWifiModule2Device](CHWifiModule2Device.md) （Wifi模块2设备）
- [CHWifiModule2MechSettings](CHWifiModule2MechSettings.md) （Wifi模块2机械设置）
- [CHWifiModule2NetWorkStatus](CHWifiModule2NetWorkStatus.md) （Wifi模块2网络状态）
- [NSError](NSError.md) （错误）
- [CHSesameBotMechSettings](CHSesameBotMechSettings.md) （SesameBot机械设置）
- [CHSesameBotMechStatus](CHSesameBotMechStatus.md) （SesameBot机械状态）
- [CHWifiModule2MechSettings](CHWifiModule2MechSettings.md) （Wifi module 2机械设置）
- [CHWifiModule2NetWorkStatus](CHWifiModule2NetWorkStatus.md) （Wifi module 2网络状态）
- [SesameOS3Payload](SesameOS3Payload.md) （SesameOS3负载）
- [CHSesameTouchCard](CHSesameTouchCard.md) （Sesame触摸卡）

## interface 列表
- [CHBaseAdv](CHBaseAdv.md) （基类设备）
- [CHDeviceUtil](CHDeviceUtil.md) （设备工具）
- [CHSesameOS3Publish](CHSesameOS3Publish.md) （SesameOS3发布）
- [CHDevices](CHDevices.md) （设备）
- [CHWifiModule2](CHWifiModule2.md) （Wifi Module 2）
- [CHDeviceStatusDelegate](CHDeviceStatusDelegate.md) （设备状态代理）
- [CHSesameTouchProDelegate](CHSesameTouchProDelegate.md) （SesameTouchPro代理）
- [CHSesameProtocolMechStatus](CHSesameProtocolMechStatus.md) （CHSesame协议机械状态）
- [CHSesameConnector](CHSesameConnector.md) （Sesame连接器）
- [CHSesameLock](CHSesameLock.md) （Sesame锁）
- [CHSesame2](CHSesame2.md) （Sesame2）
- [CHSesame5](CHSesame5.md) （Sesame5）
- [CHSesameBike](CHSesameBike.md) （Sesame自行车）
- [CHSesameBike2](CHSesameBike2.md) （Sesame自行车2）
- [CHSesameBot](CHSesameBot.md) （Sesame机器人）
- [CHSesameSensorDelegate](CHSesameSensorDelegate.md) （Sesame传感器代理）
- [CHSesameSensor](CHSesameSensor.md) （Sesame传感器）
- [CHSesameTouchPro](CHSesameTouchPro.md) （Sesame触摸专业版）
- [CHWifiModule2Delegate](CHWifiModule2Delegate.md) （Wifi module 2代理）





## typealias 列表
1. internal typealias SesameOS3ResponseCallback = (result: SSM3ResponsePayload) -> Unit  
 - `SesameOS3ResponseCallback` 是一个内部类型别名，它定义了一个函数类型，这个函数接受一个 `SSM3ResponsePayload` 类型的 `result` 参数，并且不返回任何值。。
2. typealias CHResult<T> = (Result<CHResultState<T>>) -> Unit
-  `CHResult<T>` 是一个类型别名，它定义了一个函数类型。这个函数接受一个 `Result<CHResultState<T>>` 类型的参数，并且不返回任何值。 在这个类型中，`Result` 是一个用来处理操作成功或失败的通用类，可以包含成功的值或一个异常。`CHResultState<T>` 是一个泛型类型，表示操作的结果状态。


