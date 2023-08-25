# CHSesameTouchProDelegate 接口
```svg


interface CHSesameTouchProDelegate : CHDeviceStatusDelegate {
    fun onSSM2KeysChanged(device: CHSesameConnector, ssm2keys: Map<String, ByteArray>) {}

    fun onKeyBoardReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {}
    fun onKeyBoardChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {}
    fun onKeyBoardReceiveEnd(device: CHSesameConnector) {}
    fun onKeyBoardReceiveStart(device: CHSesameConnector) {}

    fun onCardReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {}
    fun onCardChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {}
    fun onCardReceiveEnd(device: CHSesameConnector) {}
    fun onCardReceiveStart(device: CHSesameConnector) {}

    fun onFingerPrintReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {}
    fun onFingerPrintChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {}
    fun onFingerPrintReceiveEnd(device: CHSesameConnector) {}
    fun onFingerPrintReceiveStart(device: CHSesameConnector) {}
}
```
`CHSesameTouchProDelegate` 是一个设备状态代理接口，继承自 `CHDeviceStatusDelegate` 接口。除了设备状态的变化，还包含了对 Sesame Touch Pro 设备的一些操作。

## 方法

- `fun onSSM2KeysChanged(device: CHSesameConnector, ssm2keys: Map<String, ByteArray>) {}` - 当 Sesame2 的密钥发生变化时被调用。
- `fun onKeyBoardReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {}` - 当接收到键盘输入时被调用。
- `fun onKeyBoardChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {}` - 当键盘状态发生变化时被调用。
- `fun onKeyBoardReceiveEnd(device: CHSesameConnector) {}` - 当键盘接收结束时被调用。
- `fun onKeyBoardReceiveStart(device: CHSesameConnector) {}` - 当开始接收键盘输入时被调用。
- `fun onCardReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {}` - 当接收到卡片信息时被调用。
- `fun onCardChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {}` - 当卡片状态发生变化时被调用。
- `fun onCardReceiveEnd(device: CHSesameConnector) {}` - 当卡片接收结束时被调用。
- `fun onCardReceiveStart(device: CHSesameConnector) {}` - 当开始接收卡片信息时被调用。
- `fun onFingerPrintReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {}` - 当接收到指纹信息时被调用。
- `fun onFingerPrintChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {}` - 当指纹状态发生变化时被调用。
- `fun onFingerPrintReceiveEnd(device: CHSesameConnector) {}` - 当指纹接收结束时被调用。
- `fun onFingerPrintReceiveStart(device: CHSesameConnector) {}` - 当开始接收指纹信息时被调用。

