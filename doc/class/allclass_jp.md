## enum リスト

- [SSM2OpCode](SSM2OpCode_jp.md) （SSM2操作コード）
- [SesameResultCode](SesameResultCode_jp.md) （Sesame結果コード）
- [SesameItemCode](SesameItemCode_jp.md) （Sesameアイテムコード）
- [Sesame2HistoryTypeEnum](Sesame2HistoryTypeEnum_jp.md) （Sesame2履歴タイプ列挙）
- [DeviceSegmentType](DeviceSegmentType_jp.md) （デバイスセグメントタイプ）
- [CHDeviceStatus](CHDeviceStatus_jp.md) （デバイスステータス）
- [CHDeviceLoginStatus](CHDeviceLoginStatus_jp.md) （CHデバイスログインステータス）
- [WM2ActionCode](WM2ActionCode_jp.md) （WM2アクションコード）

## class リスト

- [SSM3PublishPayload](SSM3PublishPayload_jp.md) （SSM3パブリッシュペイロード）
- [SesameNotifypayload](SesameNotifypayload_jp.md) （Sesame通知ペイロード）
- [SSM2ResponsePayload](SSM2ResponsePayload_jp.md) （SSM2レスポンスペイロード）
- [SSM3ResponsePayload](SSM3ResponsePayload_jp.md) （SSM3レスポンスペイロード）
- [CHDeviceUtil](CHDeviceUtil_jp.md) （デバイスユーティリティ）
- [CHBaseDevice](CHBaseDevice_jp.md) （基本デバイス）
- [CHadv](CHadv_jp.md) （ScanResult処理）
- [SesameBleReceiver](SesameBleReceiver_jp.md) （Sesame Bluetoothレシーバー）
- [SesameBleTransmit](SesameBleTransmit_jp.md) （Sesame Bluetoothトランスミッター）
- [CHSesameBike2MechStatus](CHSesameBike2MechStatus_jp.md) （SesameBike2メカニカルステータス）
- [SesameOS3BleCipher](SesameOS3BleCipher_jp.md) （SesameOS3 Bluetooth暗号）
- [CHSesameOS3](CHSesameOS3_jp.md) （SesameOS3）
- [CHSesame5Device](CHSesame5Device_jp.md) （Sesame5デバイス）
- [CHSesameBike2Device](CHSesameBike2Device_jp.md) （SesameBike2デバイス）
- [CHSesameTouchProDevice](CHSesameTouchProDevice_jp.md) （SesameTouchProデバイス）
- [CHWifiModule2Device](CHWifiModule2Device_jp.md) （Wifiモジュール2デバイス）
- [CHWifiModule2MechSettings](CHWifiModule2MechSettings_jp.md) （Wifiモジュール2メカニカル設定）
- [CHWifiModule2NetWorkStatus](CHWifiModule2NetWorkStatus_jp.md) （Wifiモジュール2ネットワークステータス）
- [NSError](NSError_jp.md) （エラー）
- [CHSesameBotMechSettings](CHSesameBotMechSettings_jp.md) （SesameBotメカニカル設定）
- [CHSesameBotMechStatus](CHSesameBotMechStatus_jp.md) （SesameBotメカニカルステータス）
- [CHWifiModule2MechSettings](CHWifiModule2MechSettings_jp.md) （Wifiモジュール2メカニカル設定）
- [CHWifiModule2NetWorkStatus](CHWifiModule2NetWorkStatus_jp.md) （Wifiモジュール2ネットワークステータス）
- [SesameOS3Payload](SesameOS3Payload_jp.md) （SesameOS3ペイロード）
- [CHSesameTouchCard](CHSesameTouchCard_jp.md) （Sesameタッチカード）

## interface リスト

- [CHBaseAdv](CHBaseAdv_jp.md) （基本デバイス）
- [CHDeviceUtil](CHDeviceUtil_jp.md) （デバイスユーティリティ）
- [CHSesameOS3Publish](CHSesameOS3Publish_jp.md) （SesameOS3パブリッシュ）
- [CHDevices](CHDevices_jp.md) （デバイス）
- [CHWifiModule2](CHWifiModule2_jp.md) （Wifiモジュール2）
- [CHDeviceStatusDelegate](CHDeviceStatusDelegate_jp.md) （デバイスステータスデリゲート）
- [CHSesameTouchProDelegate](CHSesameTouchProDelegate_jp.md) （SesameTouchProデリゲート）
- [CHSesameProtocolMechStatus](CHSesameProtocolMechStatus_jp.md) （CHSesameプロトコルメカニカルステータス）
- [CHSesameConnector](CHSesameConnector_jp.md) （Sesameコネクタ）
- [CHSesameLock](CHSesameLock_jp.md) （Sesameロック）
- [CHSesame2](CHSesame2_jp.md) （Sesame2）
- [CHSesame5](CHSesame5_jp.md) （Sesame5）
- [CHSesameBike](CHSesameBike_jp.md) （Sesameバイク）
- [CHSesameBike2](CHSesameBike2_jp.md) （Sesameバイク2）
- [CHSesameBot](CHSesameBot_jp.md) （Sesameロボット）
- [CHSesameSensorDelegate](CHSesameSensorDelegate_jp.md) （Sesameセンサーデリゲート）
- [CHSesameSensor](CHSesameSensor_jp.md) （Sesameセンサー）
- [CHSesameTouchPro](CHSesameTouchPro_jp.md) （Sesameタッチプロ）
- [CHWifiModule2Delegate](CHWifiModule2Delegate_jp.md) （Wifiモジュール2デリゲート）


## typealias リスト

1. internal typealias SesameOS3ResponseCallback = (result: SSM3ResponsePayload) -> Unit
- `SesameOS3ResponseCallback` は内部型エイリアスで、 `SSM3ResponsePayload` 型の `result` パラメータを受け取り、何も返さない関数型を定義しています。この型の関数は通常、非同期操作の結果を処理するために使用されます。例えば、ネットワークリクエストのレスポンスなどです。具体的な処理方法は関数の具体的な実装によります。
2. typealias CHResult<T> = (Result<CHResultState<T>>) -> Unit
-  `CHResult<T>` は型エイリアスで、 `Result<CHResultState<T>>` 型のパラメータを受け取り、何も返さない関数型を定義しています。この型では、 `Result` は操作が成功したか失敗したかを処理する一般的なクラスで、成功した値または例外を含むことができます。 `CHResultState<T>` は操作の結果状態を表すジェネリック型です。
