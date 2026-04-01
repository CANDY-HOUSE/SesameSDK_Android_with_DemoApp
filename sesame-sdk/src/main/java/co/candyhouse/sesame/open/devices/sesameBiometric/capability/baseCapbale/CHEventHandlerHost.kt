package co.candyhouse.sesame.open.devices.sesameBiometric.capability.baseCapbale

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