package co.candyhouse.app.tabs.devices.hub3.adapter.provider

/**
 *
 *
 * @author frey on 2025/4/10
 */
interface Ssm2DevicesAdapterProvider {
    fun getDeviceNameByIdNew(id: String): String?

    fun removeSesame(id: String)
}