package co.candyhouse.app.ext

import java.util.concurrent.ConcurrentHashMap

/**
 * bot 脚本缓存
 *
 * @author frey on 2026/3/4
 */
object BotScriptStore {

    data class ScriptMeta(
        val alias: String? = null,
        val displayOrder: Int? = null
    )

    private val map = ConcurrentHashMap<
            String,
            ConcurrentHashMap<Int, ScriptMeta>
            >()

    fun merge(deviceUUID: String, data: Map<Int, ScriptMeta>) {
        val devKey = deviceUUID.uppercase()
        val inner = map.getOrPut(devKey) { ConcurrentHashMap() }
        data.forEach { (idx, meta) ->
            val old = inner[idx]
            inner[idx] = ScriptMeta(
                alias = meta.alias ?: old?.alias,
                displayOrder = meta.displayOrder ?: old?.displayOrder
            )
        }
    }

    fun putAlias(deviceUUID: String, actionIndex: Int, alias: String) {
        val devKey = deviceUUID.uppercase()
        val inner = map.getOrPut(devKey) { ConcurrentHashMap() }
        val old = inner[actionIndex]
        inner[actionIndex] = ScriptMeta(
            alias = alias,
            displayOrder = old?.displayOrder
        )
    }

    fun getAlias(deviceUUID: String, actionIndex: Int): String? {
        return map[deviceUUID.uppercase()]?.get(actionIndex)?.alias?.takeIf { it.isNotBlank() }
    }

    fun getDisplayOrder(deviceUUID: String, actionIndex: Int): Int? {
        return map[deviceUUID.uppercase()]?.get(actionIndex)?.displayOrder
    }

    fun clear(deviceUUID: String) {
        map.remove(deviceUUID.uppercase())
    }
}