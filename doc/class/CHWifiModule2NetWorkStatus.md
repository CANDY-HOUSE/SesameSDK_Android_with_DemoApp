
# CHWifiModule2NetWorkStatus クラス
```svg
class CHWifiModule2NetWorkStatus(
    var isAPWork: Boolean?,
    var isNetWork: Boolean?,
    var isIOTWork: Boolean?,
    var isAPConnecting: Boolean,
    var isConnectingNet: Boolean,
    var isConnectingIOT: Boolean,
    var isAPCheck: Boolean?
) : CHSesameProtocolMechStatus {
    override val data: ByteArray
    override fun getBatteryVoltage(): Float 
}


```
`CHWifiModule2NetWorkStatus`クラスは、Wifiモジュール2のネットワーク状態を処理および管理します。

## プロパティ

- `isAPWork`：APが動作しているかどうかを表します。Boolean型です。
- `isNetWork`：ネットワークがあるかどうかを表します。Boolean型です。
- `isIOTWork`：IOTが動作しているかどうかを表します。Boolean型です。
- `isAPConnecting`：APに接続しているかどうかを表します。Boolean型です。
- `isConnectingNet`：ネットワークに接続しているかどうかを表します。Boolean型です。
- `isConnectingIOT`：IOTに接続しているかどうかを表します。Boolean型です。
- `isAPCheck`：APをチェックしているかどうかを表します。Boolean型です。

## メソッド

- `getBatteryVoltage()`：電池の電圧を取得し、Float型の電圧値を返します。具体的な実装はまだ完了していませんので、今後実装を行う必要があります。

## 継承

- `CHSesameProtocolMechStatus`：`CHWifiModule2NetWorkStatus`クラスは`CHSesameProtocolMechStatus`クラスを継承し、親クラスの`data`というプロパティと`getBatteryVoltage()`というメソッドを実装する必要があります。
