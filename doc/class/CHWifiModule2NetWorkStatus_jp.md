# CHWifiModule2NetWorkStatus クラスのドキュメント

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

`CHWifiModule2NetWorkStatus` クラスは、Wifi モジュール 2 のネットワーク状態を処理および管理するためのクラスです。

## 属性

- `isAPWork`：AP（アクセスポイント）が動作しているかどうかを示す、ブール値（Boolean）型の属性です。
- `isNetWork`：ネットワークに接続されているかどうかを示す、ブール値（Boolean）型の属性です。
- `isIOTWork`：IOT（Internet of Things）が動作しているかどうかを示す、ブール値（Boolean）型の属性です。
- `isAPConnecting`：AP への接続が行われているかどうかを示す、ブール値（Boolean）型の属性です。
- `isConnectingNet`：ネットワークへの接続が行われているかどうかを示す、ブール値（Boolean）型の属性です。
- `isConnectingIOT`：IOT への接続が行われているかどうかを示す、ブール値（Boolean）型の属性です。
- `isAPCheck`：AP の状態をチェックしているかどうかを示す、ブール値（Boolean）型の属性です。

## メソッド

- `getBatteryVoltage()`：電池の電圧を取得するためのメソッドです。Float 型の電圧値を返します。具体的な実装はまだ行われておらず、後続の実装が必要です。

## 継承

- `CHSesameProtocolMechStatus`：`CHWifiModule2NetWorkStatus` クラスは、`CHSesameProtocolMechStatus` クラスを継承しており、親クラスの `data` 属性と `getBatteryVoltage()` メソッドを実装する必要があります。
