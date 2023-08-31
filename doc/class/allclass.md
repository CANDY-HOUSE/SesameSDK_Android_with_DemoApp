## enum リスト
- [SSM2OpCode](SSM2OpCode.md) （SSM2の操作コード）
- [SesameResultCode](SesameResultCode.md) （Sesameの結果コード）
- [SesameItemCode](SesameItemCode.md) （Sesameのアイテムコード）
- [Sesame2HistoryTypeEnum](Sesame2HistoryTypeEnum.md) （Sesame2の履歴タイプ列挙）
- [DeviceSegmentType](DeviceSegmentType.md) （デバイスのセグメントタイプ）
- [CHDeviceStatus](CHDeviceStatus.md) （デバイス状態）
- [CHDeviceLoginStatus](CHDeviceLoginStatus.md) （CHデバイスのログイン状態）
- [WM2ActionCode](WM2ActionCode.md) （WM2のアクションコード）

## class リスト
- [SSM3PublishPayload](SSM3PublishPayload.md) （SSM3の送信するデータ）
- [SesameNotifypayload](SesameNotifypayload.md) （Sesameの通知するデータ）
- [SSM2ResponsePayload](SSM2ResponsePayload.md) （SSM2の応答するデータ）
- [SSM3ResponsePayload](SSM3ResponsePayload.md) （SSM3の応答するデータ）
- [CHDeviceUtil](CHDeviceUtil.md) （デバイス・ユーティリティ）
- [CHBaseDevice](CHBaseDevice.md) （基本デバイス）
- [CHadv](CHadv.md) （ScanResultの処理）
- [SesameBleReceiver](SesameBleReceiver.md) （Sesameのbluetoothレシーバー）
- [SesameBleTransmit](SesameBleTransmit.md) （Sesameのbluetoothトランスミッター）
- [CHSesameBike2MechStatus](CHSesameBike2MechStatus.md) （SesameBike2の機械状態）
- [SesameOS3BleCipher](SesameOS3BleCipher.md) （SesameOS3のbluetoothパスワード）
- [CHSesameOS3](CHSesameOS3.md) （SesameOS3）
- [CHSesame5Device](CHSesame5Device.md) （Sesame5デバイス）
- [CHSesameBike2Device](CHSesameBike2Device.md) （SesameBike2デバイス）
- [CHSesameTouchProDevice](CHSesameTouchProDevice.md) （SesameTouchProデバイス）
- [CHWifiModule2Device](CHWifiModule2Device.md) （Wifiモジュール2デバイス）
- [CHWifiModule2MechSettings](CHWifiModule2MechSettings.md) （Wifiモジュール2の機械設置）
- [CHWifiModule2NetWorkStatus](CHWifiModule2NetWorkStatus.md) （Wifiモジュール2のネットワーク状態）
- [NSError](NSError.md) （エラー）
- [CHSesameBotMechSettings](CHSesameBotMechSettings.md) （SesameBotの機械設置）
- [CHSesameBotMechStatus](CHSesameBotMechStatus.md) （SesameBotの機械状態）
- [CHWifiModule2MechSettings](CHWifiModule2MechSettings.md) （Wifi module 2のの機械設置）
- [CHWifiModule2NetWorkStatus](CHWifiModule2NetWorkStatus.md) （Wifi module 2のネットワーク状態）
- [SesameOS3Payload](SesameOS3Payload.md) （SesameOS3のデータ）
- [CHSesameTouchCard](CHSesameTouchCard.md) （Sesameのタッチカード）

## interface リスト
- [CHBaseAdv](CHBaseAdv.md) （デバイスの基本アドバタイジング）
- [CHDeviceUtil](CHDeviceUtil.md) （デバイス・ユーティリティ）
- [CHSesameOS3Publish](CHSesameOS3Publish.md) （SesameOS3の送信内容）
- [CHDevices](CHDevices.md) （デバイス）
- [CHWifiModule2](CHWifiModule2.md) （Wifi Module 2）
- [CHDeviceStatusDelegate](CHDeviceStatusDelegate.md) （デバイス状態のDelegate）
- [CHSesameTouchProDelegate](CHSesameTouchProDelegate.md) （SesameTouchProのDelegate）
- [CHSesameProtocolMechStatus](CHSesameProtocolMechStatus.md) （CHSesameのプロトコルの機械状態）
- [CHSesameConnector](CHSesameConnector.md) （Sesameコネクター）
- [CHSesameLock](CHSesameLock.md) （Sesameロック）
- [CHSesame2](CHSesame2.md) （Sesame2）
- [CHSesame5](CHSesame5.md) （Sesame5）
- [CHSesameBike](CHSesameBike.md) （Sesameサイクル）
- [CHSesameBike2](CHSesameBike2.md) （Sesameサイクル2）
- [CHSesameBot](CHSesameBot.md) （Sesameボット）
- [CHSesameSensorDelegate](CHSesameSensorDelegate.md) （Sesameセンサーの代理）
- [CHSesameSensor](CHSesameSensor.md) （Sesameセンサー）
- [CHSesameTouchPro](CHSesameTouchPro.md) （Sesameタッチプロー）
- [CHWifiModule2Delegate](CHWifiModule2Delegate.md) （Wifi module 2の代理）





## typealias リスト
1. internal typealias SesameOS3ResponseCallback = (result: SSM3ResponsePayload) -> Unit  
 - `SesameOS3ResponseCallback`は、`SSM3ResponsePayload`型の`result`というパラメータを入力する関数タイプを定義し、データを返しません。
2. typealias CHResult<T> = (Result<CHResultState<T>>) -> Unit
-  `CHResult<T>` は `Result<CHResultState<T>>` 型というパラメータを入力する関数タイプを定義し、データを返しません。この型では、`Result`は操作の成功または失敗を処理するための汎用クラスで、成功した値またはエラー値が表示可能です。`CHResultState<T>`は汎用クラスで、操作結果の状態を表示します。  


