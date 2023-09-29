#  Sesame2HistoryTypeEnum 列挙型

```svg

internal enum class Sesame2HistoryTypeEnum(var value: Byte) {

    NONE(0),
    BLE_LOCK(1),
    BLE_UNLOCK(2), 
    TIME_CHANGED(3),
    AUTOLOCK_UPDATED(4),
    MECH_SETTING_UPDATED(5),
    AUTOLOCK(6),
    MANUAL_LOCKED(7), 
    MANUAL_UNLOCKED(8),
    MANUAL_ELSE(9), 
    DRIVE_LOCKED(10),
    DRIVE_UNLOCKED(11),
    DRIVE_FAILED(12), 
    BLE_ADV_PARAM_UPDATED(13), 
    WM2_LOCK(14),
    WM2_UNLOCK(15),
    WEB_LOCK(16), 
    WEB_UNLOCK(17), ;


}
```



`Sesame2HistoryTypeEnum` は内部の列挙型です。

## 列挙値

この列挙型には、多くの列挙項目が含まれており、それぞれ異なる履歴のイベントタイプに対応しています。各列挙項目は `Byte` 型の値を持っています。

以下は`Sesame2HistoryTypeEnum`列挙型の詳細なリストです。

| 命令名 | HistoryType | 説明 |
|---|---|---|
| NONE | 0 | なし |
| BLE_LOCK | 1 | Bluetoothをロックする |
| BLE_UNLOCK | 2 | Bluetoothのロックを解除する |
| TIME_CHANGED | 3 | 時間が変更された |
| AUTOLOCK_UPDATED | 4 | 自動ロックが更新された |
| MECH_SETTING_UPDATED | 5 | 機械設置が更新された |
| AUTOLOCK | 6 | 自動的にロックする |
| MANUAL_LOCKED | 7 | 手動的にロックする |
| MANUAL_UNLOCKED | 8 | 手動的にロックを解除する |
| MANUAL_ELSE | 9 | その他 |
| DRIVE_LOCKED | 10 | ドライブがロックされた |
| DRIVE_UNLOCKED | 11 | ドライブのロックが解除された |
| DRIVE_FAILED | 12 | ドライブが失敗した |
| BLE_ADV_PARAM_UPDATED | 13 | Bluetoothのアドバタイジングパラメータが更新された |
| WM2_LOCK | 14 | WM2をロックする |
| WM2_UNLOCK | 15 | WM2のロックを解除する |
| WEB_LOCK | 16 | ウェブをロックする |
| WEB_UNLOCK | 17 | ウェブのロックを解除する |

