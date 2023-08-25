
# CHSesameLock 接口
```svg
interface CHSesameLock : CHDevices {

    fun isEnableNotification(fcmToken: String, result: CHResult<Boolean>)

    fun enableNotification(fcmToken: String, result: CHResult<Any>)

    fun disableNotification(fcmToken: String, result: CHResult<Any>)
}
```

`CHSesameLock` 是一个设备接口，继承自 `CHDevices` 接口。它负责管理和操作 Sesame 锁设备。

## 方法

- `fun isEnableNotification(fcmToken: String, result: CHResult<Boolean>)` - 检查是否启用了通知。如果启用了通知，则返回 true，否则返回 false。
- `fun enableNotification(fcmToken: String, result: CHResult<Any>)` - 启用通知。如果操作成功，则返回操作结果，否则返回错误信息。
- `fun disableNotification(fcmToken: String, result: CHResult<Any>)` - 禁用通知。如果操作成功，则返回操作结果，否则返回错误信息。

