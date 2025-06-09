package co.candyhouse.sesame.open.device.sesameBiometric.devices

import co.candyhouse.sesame.ble.os3.CHRemoteNanoTriggerSettings
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHCapabilitySupport
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHEventHandler
import co.candyhouse.sesame.open.device.sesameBiometric.capability.card.CHCardDelegate
import co.candyhouse.sesame.open.device.sesameBiometric.capability.remoteNano.CHRemoteNanoCapable

interface CHSesameBiometricBase : CHSesameConnector, CHRemoteNanoCapable, CHCapabilitySupport {
    // 基础属性
    var triggerDelaySetting: CHRemoteNanoTriggerSettings?
    override var ssm2KeysMap: MutableMap<String, ByteArray>

    var radarPayload: ByteArray

    // 设备管理能力
    fun login(token: String?)
    fun goIOT()
    fun subscribeNameUpdateTopic(cardDelegate: CHCardDelegate)
    fun unsubscribeNameUpdateTopic()

    // 事件处理相关
    fun addEventHandler(handler: CHEventHandler)
    fun removeEventHandler(handler: CHEventHandler)
    fun clearEventHandlers()
}