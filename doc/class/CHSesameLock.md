
# CHSesameLock インターフェース
```svg
interface CHSesameLock : CHDevices {

    fun isEnableNotification(fcmToken: String, result: CHResult<Boolean>)

    fun enableNotification(fcmToken: String, result: CHResult<Any>)

    fun disableNotification(fcmToken: String, result: CHResult<Any>)
}
```

`CHSesameLock` は、`CHDevices` インターフェースを継承したデバイスのインターフェースです。Sesame lockデバイスを管理および操作します。

## メソッド

- `fun isEnableNotification(fcmToken: String, result: CHResult<Boolean>)` - 通知が有効になっているかどうかをチェックします。通知が有効な場合はtrueを返し、それ以外の場合はfalseを返します。
- `fun enableNotification(fcmToken: String, result: CHResult<Any>)` - 通知を有効にします。操作が成功した場合は操作結果を返し、それ以外の場合はエラーメッセージを返します。
- `fun disableNotification(fcmToken: String, result: CHResult<Any>)` - 通知を無効にします。操作が成功した場合は操作結果を返し、それ以外の場合はエラーメッセージを返します。

