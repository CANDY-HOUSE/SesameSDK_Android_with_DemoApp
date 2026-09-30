package co.candyhouse.sesame.ble.os3.biometric.capability.baseCapbale

/**
 *
 *
 * @author frey on 2026/3/31
 */
interface CHEventHandlerHost {
    fun addEventHandler(handler: CHEventHandler)
    fun removeEventHandler(handler: CHEventHandler)
    fun clearEventHandlers()
}
