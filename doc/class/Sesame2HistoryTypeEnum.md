#  Sesame2HistoryTypeEnum 枚举类

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



`Sesame2HistoryTypeEnum` 是一个内部枚举类。

## 枚举值

这个枚举类包含了许多枚举项，对应不同的历史事件类型。每个枚举项都有一个 `Byte` 类型的值。

以下是对 `Sesame2HistoryTypeEnum` 枚举类的详细列表：

| 枚举值 | 值 | 描述 |
|---|---|---|
| NONE | 0 | 无 |
| BLE_LOCK | 1 | 蓝牙锁定 |
| BLE_UNLOCK | 2 | 蓝牙解锁 |
| TIME_CHANGED | 3 | 时间已更改 |
| AUTOLOCK_UPDATED | 4 | 自动锁定已更新 |
| MECH_SETTING_UPDATED | 5 | 机械设置已更新 |
| AUTOLOCK | 6 | 自动锁定 |
| MANUAL_LOCKED | 7 | 手动锁定 |
| MANUAL_UNLOCKED | 8 | 手动解锁 |
| MANUAL_ELSE | 9 | 手动其他 |
| DRIVE_LOCKED | 10 | 驱动锁定 |
| DRIVE_UNLOCKED | 11 | 驱动解锁 |
| DRIVE_FAILED | 12 | 驱动失败 |
| BLE_ADV_PARAM_UPDATED | 13 | 蓝牙广播参数已更新 |
| WM2_LOCK | 14 | WM2锁定 |
| WM2_UNLOCK | 15 | WM2解锁 |
| WEB_LOCK | 16 | 网络锁定 |
| WEB_UNLOCK | 17 | 网络解锁 |

