# SesameItemCode  命令コード
```svg


internal enum class SesameItemCode(val value: UByte) {

    none(0u), 
    registration(1u), 
    login(2u),
    user(3u),
    history(4u),
    versionTag(5u), 
    disconnectRebootNow(6u),
    enableDFU(7u),
    time(8u),
    bleConnectionParam(9u),
    bleAdvParam(10u), 
    autolock(11u), 
    serverAdvKick(12u),
    ssmtoken(13u), 
    initial(14u),
    IRER(15u), 
    timePhone(16u),
    magnet(17u),
    BLE_ADV_PARAM_GET(18u),
    SENSOR_INVERVAL(19u), 
    SENSOR_INVERVAL_GET(20u),
    mechSetting(80u),
    mechStatus(81u),
    lock(82u),
    unlock(83u), 
    moveTo(84u), 
    driveDirection(85u),
    stop(86u), 
    detectDir(87u),
    toggle(88u),
    click(89u), 
    ADD_SESAME(101u),
    PUB_KEY_SESAME(102u), 
    REMOVE_SESAME(103u),
    Reset(104u),
    NOTIFY_LOCK_DOWN(106u),

    SSM_OS3_CARD_CHANGE(107u),
    SSM_OS3_CARD_DELETE(108u), 
    SSM_OS3_CARD_GET(109u), 
    SSM_OS3_CARD_NOTIFY(110u),
    SSM_OS3_CARD_LAST(111u), 
    SSM_OS3_CARD_FIRST(112u), 
    SSM_OS3_CARD_MODE_GET(113u),
    SSM_OS3_CARD_MODE_SET(114u),

    SSM_OS3_FINGERPRINT_CHANGE(115u),
    SSM_OS3_FINGERPRINT_DELETE(116u),
    SSM_OS3_FINGERPRINT_GET(117u),
    SSM_OS3_FINGERPRINT_NOTIFY(118u),
    SSM_OS3_FINGERPRINT_LAST(119u), 
    SSM_OS3_FINGERPRINT_FIRST(120u),
    SSM_OS3_FINGERPRINT_MODE_GET(121u),
    SSM_OS3_FINGERPRINT_MODE_SET(122u),

    SSM_OS3_PASSCODE_CHANGE(123u),
    SSM_OS3_PASSCODE_DELETE(124u), 
    SSM_OS3_PASSCODE_GET(125u), 
    SSM_OS3_PASSCODE_NOTIFY(126u),
    SSM_OS3_PASSCODE_LAST(127u),
    SSM_OS3_PASSCODE_FIRST(128u), 
    SSM_OS3_PASSCODE_MODE_GET(129u), 
    SSM_OS3_PASSCODE_MODE_SET(130u), ;
}

```
`SesameItemCode` は内部の列挙型です。

## 列挙値
この列挙型には、多くの列挙項目が含まれており、それぞれ異なるプロジェクトコードに対応しています。各列挙項目は `UByte` 型の値を持っています。

以下は `SesameItemCode` 列挙型の詳細なリストです。

| 命令名 | SesameItemCode | 説明 |
| :---: | :---: | :-- |
| none | 0u | なし |
| registration | 1u | 登録 |
| login | 2u | ログイン |
| user | 3u | ユーザー |
| history | 4u | 履歴 |
| versionTag | 5u | バージョンタグ |
| disconnectRebootNow | 6u | 直ちに切断して再起動する |
| enableDFU | 7u | DFUを有効にする |
| time | 8u | 時間 |
| bleConnectionParam | 9u | Bluetoothの接続パラメータ |
| bleAdvParam | 10u | Bluetoothのアドバタイジングパラメータ |
| autolock | 11u | 自動的にロックする |
| serverAdvKick | 12u | サーバーアドバタイジングキック |
| ssmtoken | 13u | SSMのトークン |
| initial | 14u | 初期的 |
| IRER | 15u | IRER |
| timePhone | 16u | タイムフォン |
| magnet | 17u | 磁石 |
| BLE_ADV_PARAM_GET | 18u | Bluetoothのアドバタイジングパラメータを取得する |
| SENSOR_INVERVAL | 19u | センサー間隔 |
| SENSOR_INVERVAL_GET | 20u | センサー間隔を取得する |
| mechSetting | 80u | 機械設置 |
| mechStatus | 81u | 機械状態 |
| lock | 82u | ロックする |
| unlock | 83u | ロックを解除する |
| moveTo | 84u | 移動する |
| driveDirection | 85u | ドライブ方向 |
| stop | 86u | ストップする |
| detectDir | 87u | 検出方向 |
| toggle | 88u | 切替える |
| click | 89u | クリックする |
| ADD_SESAME | 101u | sesameを添加する |
| PUB_KEY_SESAME | 102u | sesameの公開鍵 |
| REMOVE_SESAME | 103u | sesameを削除する |
| Reset | 104u | リセットする |
| NOTIFY_LOCK_DOWN | 106u | ロックを通知する |
| SSM_OS3_CARD_CHANGE | 107u | SSM_OS3のカードを変更する |
| SSM_OS3_CARD_DELETE | 108u | SSM_OS3のカードを削除する |
| SSM_OS3_CARD_GET | 109u | SSM_OS3のカードを取得する |
| SSM_OS3_CARD_NOTIFY | 110u | SSM_OS3のカードを通知する |
| SSM_OS3_CARD_LAST | 111u | 前のSSM_OS3のカード |
| SSM_OS3_CARD_FIRST | 112u | 最初のSSM_OS3のカード |
| SSM_OS3_CARD_MODE_GET | 113u | SSM_OS3のカードモデルを取得する |
| SSM_OS3_CARD_MODE_SET | 114u | SM_OS3のカードモデルを設置する |
| SSM_OS3_FINGERPRINT_CHANGE | 115u | SSM_OS3の指紋を変更する |
| SSM_OS3_FINGERPRINT_DELETE | 116u | SSM_OS3の指紋を削除する |
| SSM_OS3_FINGERPRINT_GET | 117u | SSM_OS3の指紋を取得する |
| SSM_OS3_FINGERPRINT_NOTIFY | 118u | SSM_OS3の指紋を通知する |
| SSM_OS3_FINGERPRINT_LAST | 119u | 前のSSM_OS3の指紋 |
| SSM_OS3_FINGERPRINT_FIRST | 120u | 最初のSSM_OS3の指紋 |
| SSM_OS3_FINGERPRINT_MODE_GET | 121u | SSM_OS3の指紋モデルを取得する |
| SSM_OS3_FINGERPRINT_MODE_SET | 122u | SSM_OS3の指紋モデルを設置する |
| SSM_OS3_PASSCODE_CHANGE | 123u | SSM_OS3のパスワードを変更する |
| SSM_OS3_PASSCODE_DELETE | 124u | SSM_OS3のパスワードを削除する |
| SSM_OS3_PASSCODE_GET | 125u | SSM_OS3のパスワードを取得する |
| SSM_OS3_PASSCODE_NOTIFY | 126u | SSM_OS3のパスワードを通知する |
| SSM_OS3_PASSCODE_LAST | 127u | 前のSSM_OS3のパスワード |
| SSM_OS3_PASSCODE_FIRST | 128u | 最初のSSM_OS3のパスワード |
| SSM_OS3_PASSCODE_MODE_GET | 129u | SSM_OS3のパスワードモデルを取得する |
| SSM_OS3_PASSCODE_MODE_SET | 130u | SSM_OS3のパスワードモデルを設置する |
