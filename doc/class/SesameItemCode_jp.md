# SesameItemCode 列挙型（指令）
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
`SesameItemCode` は、異なるアイテムコードに対応する内部の列挙型です。

## 列挙値

この列挙型は多くの列挙項目を含んでおり、異なるプロジェクトコードを表します。各列挙項目は `UByte` 型の値を持ちます。

以下は `SesameItemCode` 列挙型の詳細な一覧です：

| 指令名 | 指令値 | 説明 |
| :---: | :---: | :-- |
| none | 0u | なし |
| registration | 1u | 登録 |
| login | 2u | ログイン |
| user | 3u | ユーザー |
| history | 4u | 履歴 |
| versionTag | 5u | バージョンタグ |
| disconnectRebootNow | 6u | 即時切断再起動 |
| enableDFU | 7u | DFU を有効にする |
| time | 8u | 時間 |
| bleConnectionParam | 9u | BLE 接続パラメータ |
| bleAdvParam | 10u | BLE 広告パラメータ |
| autolock | 11u | 自動ロック |
| serverAdvKick | 12u | サーバー広告キック |
| ssmtoken | 13u | SSM トークン |
| initial | 14u | 初期 |
| IRER | 15u | IRER |
| timePhone | 16u | 時間（電話） |
| magnet | 17u | マグネット |
| BLE_ADV_PARAM_GET | 18u | BLE 広告パラメータを取得 |
| SENSOR_INVERVAL | 19u | センサー間隔 |
| SENSOR_INVERVAL_GET | 20u | センサー間隔を取得 |
| mechSetting | 80u | 機械設定 |
| mechStatus | 81u | 機械状態 |
| lock | 82u | ロック |
| unlock | 83u | アンロック |
| moveTo | 84u | 移動先 |
| driveDirection | 85u | 駆動方向 |
| stop | 86u | 停止 |
| detectDir | 87u | 方向の検出 |
| toggle | 88u | 切り替え |
| click | 89u | クリック |
| ADD_SESAME | 101u | Sesame を追加 |
| PUB_KEY_SESAME | 102u | 公開鍵 Sesame |
| REMOVE_SESAME | 103u | Sesame を削除 |
| Reset | 104u | リセット |
| NOTIFY_LOCK_DOWN | 106u | ロックダウンを通知 |
| SSM_OS3_CARD_CHANGE | 107u | SSM_OS3 カード変更 |
| SSM_OS3_CARD_DELETE | 108u | SSM_OS3 カード削除 |
| SSM_OS3_CARD_GET | 109u | SSM_OS3 カードを取得 |
| SSM_OS3_CARD_NOTIFY | 110u | SSM_OS3 カード通知 |
| SSM_OS3_CARD_LAST | 111u | 前の SSM_OS3 カード |
| SSM_OS3_CARD_FIRST | 112u | 最初の SSM_OS3 カード |
| SSM_OS3_CARD_MODE_GET | 113u | SSM_OS3 カードモードを取得 |
| SSM_OS3_CARD_MODE_SET | 114u | SSM_OS3 カードモードを設定 |
| SSM_OS3_FINGERPRINT_CHANGE | 115u | SSM_OS3 指紋変更 |
| SSM_OS3_FINGERPRINT_DELETE | 116u | SSM_OS3 指紋削除 |
| SSM_OS3_FINGERPRINT_GET | 117u | SSM_OS3 指紋を取得 |
| SSM_OS3_FINGERPRINT_NOTIFY | 118u | SSM_OS3 指紋通知 |
| SSM_OS3_FINGERPRINT_LAST | 119u | 前の SSM_OS3 指紋 |
| SSM_OS3_FINGERPRINT_FIRST | 120u | 最初の SSM_OS3 指紋 |
| SSM_OS3_FINGERPRINT_MODE_GET | 121u | SSM_OS3 指紋モードを取得 |
| SSM_OS3_FINGERPRINT_MODE_SET | 122u | SSM_OS3 指紋モードを設定 |
| SSM_OS3_PASSCODE_CHANGE | 123u | SSM_OS3 パスコード変更 |
| SSM_OS3_PASSCODE_DELETE | 124u | SSM_OS3 パスコード削除 |
| SSM_OS3_PASSCODE_GET | 125u | SSM_OS3 パスコードを取得 |
| SSM_OS3_PASSCODE_NOTIFY | 126u | SSM_OS3 パスコード通知 |
| SSM_OS3_PASSCODE_LAST | 127u | 前の SSM_OS3 パスコード |
| SSM_OS3_PASSCODE_FIRST | 128u | 最初の SSM_OS3 パスコード |
| SSM_OS3_PASSCODE_MODE_GET | 129u | SSM_OS3 パスコードモードを取得 |
| SSM_OS3_PASSCODE_MODE_SET | 130u | SSM_OS3 パスコードモードを設定 |
