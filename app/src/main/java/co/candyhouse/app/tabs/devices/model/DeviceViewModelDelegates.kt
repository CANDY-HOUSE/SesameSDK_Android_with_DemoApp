package co.candyhouse.app.tabs.devices.model

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import co.candyhouse.sesame.ble.os3.CHRemoteNanoTriggerSettings
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.open.device.CHHub3Delegate
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.CHWifiModule2
import co.candyhouse.sesame.open.device.CHWifiModule2Delegate
import co.candyhouse.sesame.open.device.CHWifiModule2MechSettings
import co.candyhouse.sesame.open.device.sesameBiometric.capability.connect.CHDeviceConnectDelegate
import co.candyhouse.sesame.open.device.sesameBiometric.capability.remoteNano.CHRemoteNanoDelegate
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

fun Any.bindLifecycle(lifecycleOwner: LifecycleOwner): Any {
    DeviceViewModelDelegates.bindDelegateLifecycle(this, lifecycleOwner)
    return this
}

class DeviceViewModelDelegates(private val vm: CHDeviceViewModel) : CHDeviceStatusDelegate,
    CHWifiModule2Delegate, CHRemoteNanoDelegate,
    CHHub3Delegate , CHDeviceConnectDelegate {

    companion object {
        private val delegates = ConcurrentHashMap<CHDevices, CopyOnWriteArrayList<Any>>()
        private val lifecycleMap = WeakHashMap<Any, WeakReference<LifecycleOwner>>()
        fun bindDelegateLifecycle(delegate: Any, lifecycleOwner: LifecycleOwner) {
            lifecycleMap[delegate] = WeakReference(lifecycleOwner)
            lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    delegates.forEach { (_, list) ->
                        list.remove(delegate)
                    }
                    lifecycleMap.remove(delegate)
                    owner.lifecycle.removeObserver(this)
                }
            })
        }
    }

    private fun notifyDelegates(device: CHDevices, action: (Any) -> Unit) {
        delegates[device]?.forEach(action)
    }

    fun createSsmosLockDelegateObj(): MutableMap<CHDevices, Any> {
        return object : MutableMap<CHDevices, Any> {
            override fun put(key: CHDevices, value: Any): Any {
                val list = delegates.getOrPut(key) { CopyOnWriteArrayList() }
                if (!list.contains(value)) {
                    list.add(value)
                }
                return value
            }

            override fun get(key: CHDevices): Any {
                return createMultiDelegate(key)
            }

            override fun remove(key: CHDevices): Any? {
                return delegates.remove(key)?.firstOrNull()
            }

            override val entries: MutableSet<MutableMap.MutableEntry<CHDevices, Any>>
                get() = delegates.entries.map { (key, value) ->
                    object : MutableMap.MutableEntry<CHDevices, Any> {
                        override val key: CHDevices = key
                        override val value: Any = createMultiDelegate(key)
                        override fun setValue(newValue: Any): Any =
                            throw UnsupportedOperationException()
                    }
                }.toMutableSet()
            override val keys: MutableSet<CHDevices>
                get() = delegates.keys
            override val size: Int
                get() = delegates.size
            override val values: MutableCollection<Any>
                get() = delegates.keys.map { createMultiDelegate(it) }.toMutableList()

            override fun clear() = delegates.clear()
            override fun isEmpty(): Boolean = delegates.isEmpty()
            override fun putAll(from: Map<out CHDevices, Any>) {
                from.forEach { (key, value) -> put(key, value) }
            }

            override fun containsKey(key: CHDevices): Boolean = delegates.containsKey(key)
            override fun containsValue(value: Any): Boolean =
                delegates.values.any { it.contains(value) }
        }
    }

    private fun createMultiDelegate(device: CHDevices): Any {
        return object : CHDeviceStatusDelegate, CHWifiModule2Delegate,
            CHHub3Delegate, CHRemoteNanoDelegate, CHDeviceConnectDelegate {
            // CHDeviceStatusDelegate methods
            override fun onMechStatus(device: CHDevices) {
                notifyDelegates(device) { if (it is CHDeviceStatusDelegate) it.onMechStatus(device) }
            }

            override fun onBleDeviceStatusChanged(
                device: CHDevices,
                status: CHDeviceStatus,
                shadowStatus: CHDeviceStatus?
            ) {
                notifyDelegates(device) {
                    if (it is CHDeviceStatusDelegate) it.onBleDeviceStatusChanged(
                        device,
                        status,
                        shadowStatus
                    )
                }
            }

            // CHWifiModule2Delegate methods
            override fun onAPSettingChanged(
                device: CHWifiModule2,
                settings: CHWifiModule2MechSettings
            ) {
                notifyDelegates(device) {
                    if (it is CHWifiModule2Delegate) it.onAPSettingChanged(
                        device,
                        settings
                    )
                }
            }

            override fun onSSM2KeysChanged(device: CHWifiModule2, ssm2keys: Map<String, String>) {
                notifyDelegates(device) {
                    if (it is CHWifiModule2Delegate) it.onSSM2KeysChanged(
                        device,
                        ssm2keys
                    )
                }
            }

            override fun onOTAProgress(device: CHWifiModule2, percent: Byte) {
                notifyDelegates(device) {
                    if (it is CHWifiModule2Delegate) it.onOTAProgress(
                        device,
                        percent
                    )
                }
            }

            override fun onScanWifiSID(device: CHWifiModule2, ssid: String, rssi: Short) {
                notifyDelegates(device) {
                    if (it is CHWifiModule2Delegate) it.onScanWifiSID(
                        device,
                        ssid,
                        rssi
                    )
                }
            }

            override fun onHub3BrightnessReceive(device: CHHub3, brightness: Int) {
                notifyDelegates(device) {
                    if (it is CHHub3Delegate) it.onHub3BrightnessReceive(
                        device,
                        brightness
                    )
                }
            }

            override fun onTriggerDelaySecondReceived(
                device: CHSesameConnector,
                setting: CHRemoteNanoTriggerSettings
            ) {
                notifyDelegates(device) { if (it is CHRemoteNanoDelegate) it.onTriggerDelaySecondReceived(device, setting) }
            }
            override fun onSSM2KeysChanged(device: CHSesameConnector, ssm2keys: Map<String, ByteArray>) {
                notifyDelegates(device) {
                    if (it is CHDeviceConnectDelegate) {
                        it.onSSM2KeysChanged(device, ssm2keys)
                    }
                }
            }
            override fun onRadarReceive(device: CHSesameConnector, payload: ByteArray) {
                notifyDelegates(device) {
                    if (it is CHDeviceConnectDelegate) {
                        it.onRadarReceive(device, payload)
                    }
                }
            }
        }
    }


    override fun onAPSettingChanged(device: CHWifiModule2, settings: CHWifiModule2MechSettings) {
        vm.viewModelScope.launch {
            (vm.ssmosLockDelegates[device] as? CHWifiModule2Delegate)?.onAPSettingChanged(
                device,
                settings
            )
        }
    }

    override fun onSSM2KeysChanged(device: CHWifiModule2, ssm2keys: Map<String, String>) {
        vm.viewModelScope.launch {
            (vm.ssmosLockDelegates[device] as? CHWifiModule2Delegate)?.onSSM2KeysChanged(
                device,
                ssm2keys
            )
        }
    }

    override fun onOTAProgress(device: CHWifiModule2, percent: Byte) {
        vm.viewModelScope.launch {
            (vm.ssmosLockDelegates[device] as? CHWifiModule2Delegate)?.onOTAProgress(
                device,
                percent
            )
        }
    }

    override fun onHub3BrightnessReceive(device: CHHub3, brightness: Int) {
        vm.viewModelScope.launch {
            (vm.ssmosLockDelegates[device] as? CHHub3Delegate)?.onHub3BrightnessReceive(device, brightness)
        }
    }

    override fun onScanWifiSID(device: CHWifiModule2, ssid: String, rssi: Short) {
        vm.viewModelScope.launch {
            (vm.ssmosLockDelegates[device] as? CHWifiModule2Delegate)?.onScanWifiSID(
                device,
                ssid,
                rssi
            )
        }
    }

    override fun onBleDeviceStatusChanged(
        device: CHDevices,
        status: CHDeviceStatus,
        shadowStatus: CHDeviceStatus?
    ) {
        L.d(
            "[say]",
            "[onBleDeviceStatusChanged][device: $device][status: $status][shadowStatus: $shadowStatus]"
        )

        /* viewModelScope.launch {
             (ssmosLockDelegates[device] as? CHWifiModule2Delegate)?.onBleDeviceStatusChanged(device, status, shadowStatus)
         }*/
        vm.viewModelScope.launch {
            (vm.ssmosLockDelegates[device] as? CHDeviceStatusDelegate)?.onBleDeviceStatusChanged(
                device,
                status,
                shadowStatus
            )
        }
        vm.backgroundAutoConnect(device)
        vm.updateWidgets(device.deviceId.toString())
    }

    override fun onMechStatus(device: CHDevices) {
        vm.viewModelScope.launch {
            (vm.ssmosLockDelegates[device] as? CHDeviceStatusDelegate)?.onMechStatus(device)
        }
    }

    override fun onTriggerDelaySecondReceived(device: CHSesameConnector, setting: CHRemoteNanoTriggerSettings) {
        vm.viewModelScope.launch {
            (vm.ssmosLockDelegates[device] as? CHRemoteNanoDelegate)?.onTriggerDelaySecondReceived(device, setting)
        }
    }

    override fun onSSM2KeysChanged(
        device: CHSesameConnector,
        ssm2keys: Map<String, ByteArray>
    ) {
        (vm.ssmosLockDelegates[device] as? CHDeviceConnectDelegate)?.onSSM2KeysChanged(device, ssm2keys)
    }

    override fun onRadarReceive(
        device: CHSesameConnector,
        payload: ByteArray
    ) {
        (vm.ssmosLockDelegates[device] as? CHDeviceConnectDelegate)?.onRadarReceive(device, payload)
    }
}