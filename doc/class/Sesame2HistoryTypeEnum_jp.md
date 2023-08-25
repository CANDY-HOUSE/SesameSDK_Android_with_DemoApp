# Sesame2HistoryTypeEnum 列挙型

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
`Sesame2HistoryTypeEnum` は、異なる履歴イベントタイプに対応する内部の列挙型です。

## 列挙値

この列挙型は多くの列挙項目を含んでおり、異なる履歴イベントに対応します。各列挙項目は `Byte` 型の値を持ちます。

以下は `Sesame2HistoryTypeEnum` 列挙型の詳細な一覧です：

| 列挙値 | 値 | 説明 |
|---|---|---|
| NONE | 0 | なし |
| BLE_LOCK | 1 | BLE ロック |
| BLE_UNLOCK | 2 | BLE 解錠 |
| TIME_CHANGED | 3 | 時間が変更されました |
| AUTOLOCK_UPDATED | 4 | 自動ロックが更新されました |
| MECH_SETTING_UPDATED | 5 | 机械設定が更新されました |
| AUTOLOCK | 6 | 自動ロック |
| MANUAL_LOCKED | 7 | 手動でロックされました |
| MANUAL_UNLOCKED | 8 | 手動で解錠されました |
| MANUAL_ELSE | 9 | 手動のその他の操作 |
| DRIVE_LOCKED | 10 | ドライブでロックされました |
| DRIVE_UNLOCKED | 11 | ドライブで解錠されました |
| DRIVE_FAILED | 12 | ドライブが失敗しました |
| BLE_ADV_PARAM_UPDATED | 13 | BLE 広告パラメータが更新されました |
| WM2_LOCK | 14 | WM2 ロック |
| WM2_UNLOCK | 15 | WM2 解錠 |
| WEB_LOCK | 16 | Web ロック |
| WEB_UNLOCK | 17 | Web 解錠 |
