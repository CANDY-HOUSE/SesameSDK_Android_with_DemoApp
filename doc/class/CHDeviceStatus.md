# CHDeviceStatus 枚举类

```svg


enum class CHDeviceStatus(val value: CHDeviceLoginStatus) {
    NoBleSignal(CHDeviceLoginStatus.UnLogin),
    ReceivedAdV(CHDeviceLoginStatus.UnLogin),
    BleConnecting(CHDeviceLoginStatus.UnLogin),
    DiscoverServices(CHDeviceLoginStatus.UnLogin),
    BleLogining(CHDeviceLoginStatus.UnLogin),
    Registering(CHDeviceLoginStatus.UnLogin),
    ReadyToRegister(CHDeviceLoginStatus.UnLogin),
    WaitingForAuth(CHDeviceLoginStatus.UnLogin),
    NoSettings(CHDeviceLoginStatus.Login),
    Reset(CHDeviceLoginStatus.UnLogin),
    DfuMode(CHDeviceLoginStatus.UnLogin),
    Busy(CHDeviceLoginStatus.UnLogin),
    Locked(CHDeviceLoginStatus.Login),
    Moved(CHDeviceLoginStatus.Login),
    Unlocked(CHDeviceLoginStatus.Login),
    WaitApConnect(CHDeviceLoginStatus.Login),
    IotConnected(CHDeviceLoginStatus.Login),
    IotDisconnected(CHDeviceLoginStatus.Login),
}
```



`CHDeviceStatus` 是一个枚举类，表示设备的状态。每个枚举常量都关联一个 `CHDeviceLoginStatus` 类型的值。

以下是每个枚举常量的含义：

| 枚举常量 | 含义 | 关联的登录状态 |
| :----- | :----- | :----- |
| `NoBleSignal` | 没有蓝牙信号 | `UnLogin` |
| `ReceivedAdV` | 收到广告 | `UnLogin` |
| `BleConnecting` | 正在连接蓝牙 | `UnLogin` |
| `DiscoverServices` | 正在发现服务 | `UnLogin` |
| `BleLogining` | 正在登录蓝牙 | `UnLogin` |
| `Registering` | 正在注册 | `UnLogin` |
| `ReadyToRegister` | 准备注册 | `UnLogin` |
| `WaitingForAuth` | 等待授权 | `UnLogin` |
| `NoSettings` | 没有设置 | `Login` |
| `Reset` | 重置 | `UnLogin` |
| `DfuMode` | 设备固件更新模式 | `UnLogin` |
| `Busy` | 设备忙 | `UnLogin` |
| `Locked` | 设备已锁定 | `Login` |
| `Moved` | 设备已移动 | `Login` |
| `Unlocked` | 设备已解锁 | `Login` |
| `WaitApConnect` | 等待接入点连接 | `Login` |
| `IotConnected` | IoT已连接 | `Login` |
| `IotDisconnected` | IoT已断开连接 | `Login` |

注意，每个状态都与设备的登录状态 (`CHDeviceLoginStatus`) 关联，表示设备在该状态下的登录情况。
