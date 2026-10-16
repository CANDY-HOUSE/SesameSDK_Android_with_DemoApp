package co.candyhouse.app.ble

import co.candyhouse.sesame.ble.CHDevices
import co.candyhouse.sesame.ble.os3.biometric.capability.baseCapbale.CHCapabilityHost
import co.candyhouse.sesame.ble.os3.biometric.capability.card.CHCardCapable
import co.candyhouse.sesame.ble.os3.biometric.capability.card.CHCardDelegate
import co.candyhouse.sesame.ble.os3.biometric.capability.face.CHFaceCapable
import co.candyhouse.sesame.ble.os3.biometric.capability.face.CHFaceDelegate
import co.candyhouse.sesame.ble.os3.biometric.capability.fingerPrint.CHFingerPrintCapable
import co.candyhouse.sesame.ble.os3.biometric.capability.fingerPrint.CHFingerPrintDelegate
import co.candyhouse.sesame.ble.os3.biometric.capability.palm.CHPalmCapable
import co.candyhouse.sesame.ble.os3.biometric.capability.palm.CHPalmDelegate
import co.candyhouse.sesame.ble.os3.biometric.capability.passcode.CHPassCodeCapable
import co.candyhouse.sesame.ble.os3.biometric.capability.passcode.CHPassCodeDelegate
import co.candyhouse.sesame.ble.os3.biometric.parseData.CHSesameTouchFace
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult

/** Device notifications of one credential list, shared by all five capabilities. */
internal interface CredentialEvents {
    fun received(id: String, name: String, type: Byte, changed: Boolean)
    fun started()
    fun ended()
    fun modeChanged(mode: Byte)
    fun deleted(id: String)
    fun deleteFailed()
}

/** Maps one credential kind onto its SDK capability; list and session semantics stay in CredentialSession. */
internal abstract class CredentialChannel(protected val device: CHDevices) {
    protected val host = device as CHCapabilityHost

    /** Mode restored when the session closes; the only accepted mode besides enrolment (1). */
    open val idleMode: Byte = 0

    /** Face/palm deletion completes on its device notification, not on the command ACK. */
    open val deleteConfirmedByEvent = false

    /** Registers the SDK delegate and returns its removal. */
    abstract fun attach(events: CredentialEvents): () -> Unit
    abstract fun readMode(result: CHResult<Byte>)
    abstract fun setMode(mode: Byte, result: CHResult<CHEmpty>)

    /** [acknowledged] is reported only by kinds whose empty list can ACK without FIRST/LAST. */
    abstract fun requestList(acknowledged: () -> Unit, failed: () -> Unit)
    open fun rename(id: String, name: String, result: CHResult<CHEmpty>): Unit = error("Name is managed in Biz")
    abstract fun delete(id: String, result: CHResult<CHEmpty>)

    companion object {
        fun of(device: CHDevices, kind: String): CredentialChannel = when (kind) {
            "card" -> Card(device)
            "fingerprint" -> FingerPrint(device)
            "passcode" -> PassCode(device)
            "face" -> Face(device)
            "palm" -> Palm(device)
            else -> error("Unsupported credential kind")
        }
    }
}

private class Card(device: CHDevices) : CredentialChannel(device) {
    private val capable = device as CHCardCapable
    override fun attach(events: CredentialEvents): () -> Unit {
        val listener = object : CHCardDelegate {
            override fun onCardReceive(device: CHDevices, cardID: String, hexName: String, type: Byte) = events.received(cardID, hexName, type, false)
            override fun onCardChanged(device: CHDevices, cardID: String, hexName: String, type: Byte) = events.received(cardID, hexName, type, true)
            override fun onCardReceiveStart(device: CHDevices) = events.started()
            override fun onCardReceiveEnd(device: CHDevices) = events.ended()
            override fun onCardModeChanged(device: CHDevices, mode: Byte) = events.modeChanged(mode)
            override fun onCardDelete(device: CHDevices, cardID: String) = events.deleted(cardID)
        }
        capable.registerEventDelegate(host, listener)
        return { capable.unregisterEventDelegate(host, listener) }
    }

    override fun readMode(result: CHResult<Byte>) = capable.cardModeGet(result)
    override fun setMode(mode: Byte, result: CHResult<CHEmpty>) = capable.cardModeSet(mode, result)
    override fun requestList(acknowledged: () -> Unit, failed: () -> Unit) =
        capable.sendNfcCardsDataGetCmd(device.deviceId.toString().uppercase()) { result -> result.onSuccess { acknowledged() }.onFailure { failed() } }

    override fun rename(id: String, name: String, result: CHResult<CHEmpty>) = capable.cardChange(id, name, result)
    override fun delete(id: String, result: CHResult<CHEmpty>) = capable.cardDelete(id, result)
}

private class FingerPrint(device: CHDevices) : CredentialChannel(device) {
    private val capable = device as CHFingerPrintCapable
    override val idleMode: Byte = 2
    override fun attach(events: CredentialEvents): () -> Unit {
        val listener = object : CHFingerPrintDelegate {
            override fun onFingerPrintReceive(device: CHDevices, ID: String, hexName: String, type: Byte) = events.received(ID, hexName, type, false)
            override fun onFingerPrintChanged(device: CHDevices, ID: String, hexName: String, type: Byte) = events.received(ID, hexName, type, true)
            override fun onFingerPrintReceiveStart(device: CHDevices) = events.started()
            override fun onFingerPrintReceiveEnd(device: CHDevices) = events.ended()
            override fun onFingerModeChange(device: CHDevices, mode: Byte) = events.modeChanged(mode)
            override fun onFingerDelete(device: CHDevices, ID: String) = events.deleted(ID)
        }
        capable.registerEventDelegate(host, listener)
        return { capable.unregisterEventDelegate(host, listener) }
    }

    override fun readMode(result: CHResult<Byte>) = capable.fingerPrintModeGet(result)
    override fun setMode(mode: Byte, result: CHResult<CHEmpty>) = capable.fingerPrintModeSet(mode, result)
    override fun requestList(acknowledged: () -> Unit, failed: () -> Unit) = capable.fingerPrints { result -> result.onFailure { failed() } }
    override fun rename(id: String, name: String, result: CHResult<CHEmpty>) = capable.fingerPrintsChange(id, name, result)
    override fun delete(id: String, result: CHResult<CHEmpty>) = capable.fingerPrintDelete(id, device.deviceId.toString(), result)
}

private class PassCode(device: CHDevices) : CredentialChannel(device) {
    private val capable = device as CHPassCodeCapable
    override fun attach(events: CredentialEvents): () -> Unit {
        val listener = object : CHPassCodeDelegate {
            override fun onKeyBoardReceive(device: CHDevices, ID: String, hexName: String, type: Byte) = events.received(ID, hexName, type, false)
            override fun onKeyBoardChanged(device: CHDevices, ID: String, hexName: String, type: Byte) = events.received(ID, hexName, type, true)
            override fun onKeyBoardReceiveStart(device: CHDevices) = events.started()
            override fun onKeyBoardReceiveEnd(device: CHDevices) = events.ended()
            override fun onKeyBoardModeChange(device: CHDevices, mode: Byte) = events.modeChanged(mode)
            override fun onKeyBoardDelete(device: CHDevices, ID: String) = events.deleted(ID)
        }
        capable.registerEventDelegate(host, listener)
        return { capable.unregisterEventDelegate(host, listener) }
    }

    override fun readMode(result: CHResult<Byte>) = capable.keyBoardPassCodeModeGet(result)
    override fun setMode(mode: Byte, result: CHResult<CHEmpty>) = capable.keyBoardPassCodeModeSet(mode, result)
    override fun requestList(acknowledged: () -> Unit, failed: () -> Unit) =
        capable.sendKeyBoardPassCodeDataGetCmd { result -> result.onSuccess { acknowledged() }.onFailure { failed() } }

    override fun rename(id: String, name: String, result: CHResult<CHEmpty>) = capable.keyBoardPassCodeChange(id, name, result)
    override fun delete(id: String, result: CHResult<CHEmpty>) = capable.keyBoardPassCodeDelete(id, device.deviceId.toString(), result)
}

private fun byteId(value: Byte) = "%02x".format(value.toInt() and 255)

private class Face(device: CHDevices) : CredentialChannel(device) {
    private val capable = device as CHFaceCapable
    override val deleteConfirmedByEvent = true
    override fun attach(events: CredentialEvents): () -> Unit {
        val listener = object : CHFaceDelegate {
            override fun onFaceReceive(device: CHDevices, face: CHSesameTouchFace) = events.received(face.id, face.nameUUID, face.type, false)
            override fun onFaceChanged(device: CHDevices, face: CHSesameTouchFace) = events.received(face.id, face.nameUUID, face.type, true)
            override fun onFaceReceiveStart(device: CHDevices) = events.started()
            override fun onFaceReceiveEnd(device: CHDevices) = events.ended()
            override fun onFaceModeChanged(device: CHDevices, mode: Byte) = events.modeChanged(mode)
            override fun onFaceDeleted(device: CHDevices, faceID: Byte, isSuccess: Boolean) =
                if (isSuccess) events.deleted(byteId(faceID)) else events.deleteFailed()
        }
        capable.registerEventDelegate(host, listener)
        return { capable.unregisterEventDelegate(host, listener) }
    }

    override fun readMode(result: CHResult<Byte>) = capable.faceModeGet(result)
    override fun setMode(mode: Byte, result: CHResult<CHEmpty>) = capable.faceModeSet(mode, result)
    override fun requestList(acknowledged: () -> Unit, failed: () -> Unit) = capable.faceListGet { result -> result.onFailure { failed() } }
    override fun delete(id: String, result: CHResult<CHEmpty>) = capable.faceDelete(id, device.deviceId.toString(), result)
}

private class Palm(device: CHDevices) : CredentialChannel(device) {
    private val capable = device as CHPalmCapable
    override val deleteConfirmedByEvent = true
    override fun attach(events: CredentialEvents): () -> Unit {
        val listener = object : CHPalmDelegate {
            override fun onPalmReceive(device: CHDevices, tochface: CHSesameTouchFace) = events.received(tochface.id, tochface.nameUUID, tochface.type, false)
            override fun onPalmChanged(device: CHDevices, tochface: CHSesameTouchFace) = events.received(tochface.id, tochface.nameUUID, tochface.type, true)
            override fun onPalmReceiveStart(device: CHDevices) = events.started()
            override fun onPalmReceiveEnd(device: CHDevices) = events.ended()
            override fun onPalmModeChanged(device: CHDevices, mode: Byte) = events.modeChanged(mode)
            override fun onPalmDeleted(device: CHDevices, palmID: Byte, isSuccess: Boolean) =
                if (isSuccess) events.deleted(byteId(palmID)) else events.deleteFailed()
        }
        capable.registerEventDelegate(host, listener)
        return { capable.unregisterEventDelegate(host, listener) }
    }

    override fun readMode(result: CHResult<Byte>) = capable.palmModeGet(result)
    override fun setMode(mode: Byte, result: CHResult<CHEmpty>) = capable.palmModeSet(mode, result)
    override fun requestList(acknowledged: () -> Unit, failed: () -> Unit) = capable.palmListGet { result -> result.onFailure { failed() } }
    override fun delete(id: String, result: CHResult<CHEmpty>) = capable.palmDelete(id, device.deviceId.toString(), result)
}
