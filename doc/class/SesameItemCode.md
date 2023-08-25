# SesameItemCode  名称(指令)
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
`SesameItemCode` 是一个内部枚举类

## 枚举值
这个枚举类包含了许多枚举项，对应不同的项目代码。每个枚举项都有一个 `UByte` 类型的值。

以下是对 `SesameItemCode` 枚举类的详细列表：

| 指令名称 | 指令值 | 描述 |
| :---: | :---: | :-- |
| none | 0u | 无 |
| registration | 1u | 注册 |
| login | 2u | 登录 |
| user | 3u | 用户 |
| history | 4u | 历史 |
| versionTag | 5u | 版本标签 |
| disconnectRebootNow | 6u | 立即断开重启 |
| enableDFU | 7u | 启用DFU |
| time | 8u | 时间 |
| bleConnectionParam | 9u | 蓝牙连接参数 |
| bleAdvParam | 10u | 蓝牙广播参数 |
| autolock | 11u | 自动锁定 |
| serverAdvKick | 12u | 服务器广告踢 |
| ssmtoken | 13u | SSM令牌 |
| initial | 14u | 初始的 |
| IRER | 15u | IRER |
| timePhone | 16u | 时间手机 |
| magnet | 17u | 磁铁 |
| BLE_ADV_PARAM_GET | 18u | 获取蓝牙广播参数 |
| SENSOR_INVERVAL | 19u | 传感器间隔 |
| SENSOR_INVERVAL_GET | 20u | 获取传感器间隔 |
| mechSetting | 80u | 机械设置 |
| mechStatus | 81u | 机械状态 |
| lock | 82u | 锁定 |
| unlock | 83u | 解锁 |
| moveTo | 84u | 移动到 |
| driveDirection | 85u | 驱动方向 |
| stop | 86u | 停止 |
| detectDir | 87u | 检测方向 |
| toggle | 88u | 切换 |
| click | 89u | 点击 |
| ADD_SESAME | 101u | 添加sesame |
| PUB_KEY_SESAME | 102u | 公钥sesame |
| REMOVE_SESAME | 103u | 移除sesame |
| Reset | 104u | 重置 |
| NOTIFY_LOCK_DOWN | 106u | 通知锁定 |
| SSM_OS3_CARD_CHANGE | 107u | SSM_OS3卡片更改 |
| SSM_OS3_CARD_DELETE | 108u | SSM_OS3卡片删除 |
| SSM_OS3_CARD_GET | 109u | 获取SSM_OS3卡片 |
| SSM_OS3_CARD_NOTIFY | 110u | SSM_OS3卡片通知 |
| SSM_OS3_CARD_LAST | 111u | 上一个SSM_OS3卡片 |
| SSM_OS3_CARD_FIRST | 112u | 第一个SSM_OS3卡片 |
| SSM_OS3_CARD_MODE_GET | 113u | 获取SSM_OS3卡片模式 |
| SSM_OS3_CARD_MODE_SET | 114u | 设置SSM_OS3卡片模式 |
| SSM_OS3_FINGERPRINT_CHANGE | 115u | SSM_OS3指纹更改 |
| SSM_OS3_FINGERPRINT_DELETE | 116u | SSM_OS3指纹删除 |
| SSM_OS3_FINGERPRINT_GET | 117u | 获取SSM_OS3指纹 |
| SSM_OS3_FINGERPRINT_NOTIFY | 118u | SSM_OS3指纹通知 |
| SSM_OS3_FINGERPRINT_LAST | 119u | 上一个SSM_OS3指纹 |
| SSM_OS3_FINGERPRINT_FIRST | 120u | 第一个SSM_OS3指纹 |
| SSM_OS3_FINGERPRINT_MODE_GET | 121u | 获取SSM_OS3指纹模式 |
| SSM_OS3_FINGERPRINT_MODE_SET | 122u | 设置SSM_OS3指纹模式 |
| SSM_OS3_PASSCODE_CHANGE | 123u | SSM_OS3密码更改 |
| SSM_OS3_PASSCODE_DELETE | 124u | SSM_OS3密码删除 |
| SSM_OS3_PASSCODE_GET | 125u | 获取SSM_OS3密码 |
| SSM_OS3_PASSCODE_NOTIFY | 126u | SSM_OS3密码通知 |
| SSM_OS3_PASSCODE_LAST | 127u | 上一个SSM_OS3密码 |
| SSM_OS3_PASSCODE_FIRST | 128u | 第一个SSM_OS3密码 |
| SSM_OS3_PASSCODE_MODE_GET | 129u | 获取SSM_OS3密码模式 |
| SSM_OS3_PASSCODE_MODE_SET | 130u | 设置SSM_OS3密码模式 |
