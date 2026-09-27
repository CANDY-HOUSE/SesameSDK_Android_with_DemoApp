package co.candyhouse.app.shortcut

object SesameShortcutContract {
    const val ACTION_EXECUTE = "co.candyhouse.sesame2.action.EXECUTE_SHORTCUT"
    const val EXTRA_DEVICE_ID = "co.candyhouse.sesame2.extra.DEVICE_ID"
    const val EXTRA_ACTION = "co.candyhouse.sesame2.extra.SHORTCUT_ACTION"
    const val EXTRA_AUTH_TOKEN = "co.candyhouse.sesame2.extra.SHORTCUT_TOKEN"

    private const val SHORTCUT_ID_PREFIX = "sesame_shortcut"
    private const val TOKEN_VERSION = "v1"

    fun shortcutId(deviceId: String, action: SesameShortcutAction): String =
        "$SHORTCUT_ID_PREFIX:${deviceId.lowercase()}:${action.wireName}"

    /** Bound to (version, deviceId, action) so tampering with either extra invalidates the token. */
    fun tokenMessage(deviceId: String, action: SesameShortcutAction): ByteArray =
        "$TOKEN_VERSION:${deviceId.lowercase()}:${action.wireName}".toByteArray(Charsets.UTF_8)
}

/** [wireName] is persisted in shortcuts and must remain stable. */
enum class SesameShortcutAction(val wireName: String) {
    LOCK("lock"),
    UNLOCK("unlock"),
    TOGGLE("toggle"),
    CLICK("click");

    companion object {
        fun fromWireName(value: String?): SesameShortcutAction? =
            entries.firstOrNull { it.wireName == value }
    }
}
