# CHDeviceStatus 列挙型クラス

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



`CHDeviceStatus`は、デバイスの状態を表す列挙型で、各列挙定数が `CHDeviceLoginStatus` 型の値と関連しています。

以下は各列挙定数の意味です

| 列挙定数 | 意味 | 関連するログイン状態 |
| :----- | :----- | :----- |
| `NoBleSignal` | Bluetooth信号がない | `UnLogin` |
| `ReceivedAdV` | アドバタイジングを受信する | `UnLogin` |
| `BleConnecting` | Bluetoothに接続している | `UnLogin` |
| `DiscoverServices` | サービスを検出する | `UnLogin` |
| `BleLogining` | Bluetoothをログインしている | `UnLogin` |
| `Registering` | 登録している | `UnLogin` |
| `ReadyToRegister` | 登録の準備をする | `UnLogin` |
| `WaitingForAuth` | 承認を待っている | `UnLogin` |
| `NoSettings` | 設置がない | `Login` |
| `Reset` | リセット | `UnLogin` |
| `DfuMode` | デバイスのファームウェアの更新モデル | `UnLogin` |
| `Busy` | ビジー | `UnLogin` |
| `Locked` | デバイスがロックされた| `Login` |
| `Moved` | デバイスが移動された | `Login` |
| `Unlocked` | デバイスのロックが解除された | `Login` |
| `WaitApConnect` | アクセスポイントの接続を待つ | `Login` |
| `IotConnected` | IoTが接続している | `Login` |
| `IotDisconnected` | IoTの接続が切断されている | `Login` |

各列挙定数はデバイスのログイン状態（`CHDeviceLoginStatus`）と関連しており、その状態でのデバイスのログイン状況を表していることにご注意ください。