# CHDeviceStatus 列挙型

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
`CHDeviceStatus` は、デバイスの状態を表す列挙型です。各列挙定数は `CHDeviceLoginStatus` 型の値と関連付けられています。

以下は各列挙定数の意味です：

| 列挙定数 | 意味 | 関連するログイン状態 |
| :----- | :----- | :----- |
| `NoBleSignal` | ブルートゥース信号なし | `UnLogin` |
| `ReceivedAdV` | 広告を受信 | `UnLogin` |
| `BleConnecting` | ブルートゥース接続中 | `UnLogin` |
| `DiscoverServices` | サービスを検出中 | `UnLogin` |
| `BleLogining` | ブルートゥースログイン中 | `UnLogin` |
| `Registering` | 登録中 | `UnLogin` |
| `ReadyToRegister` | 登録の準備完了 | `UnLogin` |
| `WaitingForAuth` | 承認待ち | `UnLogin` |
| `NoSettings` | 設定なし | `Login` |
| `Reset` | リセット | `UnLogin` |
| `DfuMode` | デバイスファームウェア更新モード | `UnLogin` |
| `Busy` | デバイスビジー | `UnLogin` |
| `Locked` | デバイスロック済み | `Login` |
| `Moved` | デバイスが移動済み | `Login` |
| `Unlocked` | デバイスアンロック済み | `Login` |
| `WaitApConnect` | アクセスポイント接続待ち | `Login` |
| `IotConnected` | IoT接続済み | `Login` |
| `IotDisconnected` | IoT接続解除済み | `Login` |

注意: 各状態は、デバイスのログイン状態 (`CHDeviceLoginStatus`) と関連付けられ、その状態でのデバイスのログイン状況を表します。
