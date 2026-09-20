package co.candyhouse.sesame.utils

/**
 * 旧 Hub 3 的 uuid 是固定前缀 + BLE MAC 拼出来的, 它的 IoT topic / thing shadow name 里只带末段 MAC;
 * Hub 3 Pro 的 uuid 取消前缀, 固件用的是完整 uuid。
 *
 * 与后台 lambda、biz3 前端的 hub3TopicId() 保持同一套约定。
 */
private const val HUB3_LEGACY_UUID_PREFIX = "00000000-055A-FD81-0D00-"

/**
 * 取 Hub3 在 IoT topic / thing shadow name 中使用的设备标识。
 *
 * 大小写由调用方决定 —— 本函数不改变返回值的大小写, 只在判定时忽略大小写。
 *
 * @param deviceUuid 带连字符的设备 uuid
 * @return 旧 Hub 3 返回末段 MAC; Hub 3 Pro 原样返回完整 uuid
 */
fun hub3TopicId(deviceUuid: String?): String {
    val raw = deviceUuid ?: return ""
    return if (raw.uppercase().startsWith(HUB3_LEGACY_UUID_PREFIX)) raw.substringAfterLast('-') else raw
}
