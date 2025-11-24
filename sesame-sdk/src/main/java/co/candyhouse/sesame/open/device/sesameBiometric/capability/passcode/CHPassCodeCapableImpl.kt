package co.candyhouse.sesame.open.device.sesameBiometric.capability.passcode

import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.StpItemCode
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.open.CHAccountManager
import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.open.CHResultState
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHAutoInitCapabilityImpl
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHDataSynchronizeCapable
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHDataSynchronizeCapableImpl
import co.candyhouse.sesame.open.device.sesameBiometric.devices.CHSesameBiometricBase
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.server.dto.CHKeyBoardPassCodeNameRequest
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.padEnd
import co.candyhouse.sesame.utils.toHexString
import co.candyhouse.sesame.utils.toReverseBytes
import java.lang.Thread.sleep
import java.util.concurrent.CountDownLatch
import kotlin.collections.set

internal open class CHPassCodeCapableImpl() :
    CHAutoInitCapabilityImpl(),
    CHPassCodeCapable {

    private val eventHandlers = mutableMapOf<CHPassCodeDelegate, CHPassCodeEventHandler>()

    override fun keyBoardPassCodeModeGet(result: CHResult<Byte>) {
        sendCommandSafely(SesameOS3Payload(SesameItemCode.SSM_OS3_PASSCODE_MODE_GET.value, byteArrayOf()), result) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(res.payload[0])))
        }
    }

    override fun keyBoardPassCodeModeSet(mode: Byte, result: CHResult<CHEmpty>) {
        sendCommandSafely(SesameOS3Payload(SesameItemCode.SSM_OS3_PASSCODE_MODE_SET.value, byteArrayOf(mode)), result) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun keyBoardPassCodeAdd(id: ByteArray, hexName: String, result: CHResult<CHEmpty>) {
        sendCommandSafely(
            SesameOS3Payload(
                SesameItemCode.SSM_OS3_PASSCODE_ADD.value,
                byteArrayOf(0xF0/*KB_DATA_USED*/.toByte()) + byteArrayOf(0x00/*KB_TYPE_CLOUD*/.toByte()) + byteArrayOf(id.size.toByte()) + id.padEnd(16, 0x00.toByte()) + byteArrayOf(hexName.toByteArray().size.toByte()) + hexName.toByteArray().padEnd(16, 0x00.toByte())
            ), result
        ) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun keyBoardPassCodeBatchAdd(data: ByteArray, progressCallback: ((current: Int, total: Int) -> Unit)?, result: CHResult<CHEmpty>) {
        Thread {
            try {
                val dataSize: Short = data.size.toShort()
                var dataIndex: Short = 0.toShort()
                val MAX_PAYLOAD_SIZE = 209

                // 计算总包数（向上取整）
                val totalPackets = (dataSize + MAX_PAYLOAD_SIZE - 1) / MAX_PAYLOAD_SIZE
                var currentPacket = 0

                L.d("sf", "totalPackets:$totalPackets dataSize:" + dataSize.toReverseBytes().toHexString())

                while (dataIndex < dataSize) {
                    currentPacket++

                    // 通知进度
                    progressCallback?.invoke(currentPacket, totalPackets)

                    val tempList = mutableListOf<Byte>()
                    tempList.addAll(dataIndex.toReverseBytes().toList())
                    tempList.addAll(dataSize.toReverseBytes().toList())

                    val remainingSize = dataSize - dataIndex
                    val chunkSize = minOf(remainingSize, MAX_PAYLOAD_SIZE)

                    tempList.addAll(data.slice(dataIndex until (dataIndex + chunkSize)))
                    dataIndex = (dataIndex + chunkSize).toShort()

                    val batchData = tempList.toByteArray()
                    L.d("sf", "Packet $currentPacket/$totalPackets - size: ${batchData.size}")

                    val latch = CountDownLatch(1)
                    var sendSuccess = false

                    sendCommandSafely(
                        SesameOS3Payload(StpItemCode.STP_ITEM_CODE_PASSCODES_ADD.value, batchData),
                        result
                    ) {
                        sendSuccess = true
                        latch.countDown()
                    }

                    latch.await()

                    if (!sendSuccess) {
                        result.invoke(Result.failure(Exception("Failed at index $currentPacket")))
                        return@Thread
                    }

                    // 如果还有数据要发送，延迟4秒
                    if (dataIndex < dataSize) {
                        sleep(4000)
                    }
                }

                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            } catch (e: Exception) {
                result.invoke(Result.failure(e))
            }
        }.start()
    }

    override fun keyBoardPassCodeDelete(keyBoardPassCodeID: String, deviceId: String, result: CHResult<CHEmpty>) {
        sendCommandSafely(SesameOS3Payload(SesameItemCode.SSM_OS3_PASSCODE_DELETE.value, keyBoardPassCodeID.hexStringToByteArray()), result) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun keyBoardPassCodeMove(cardId: String, touchProUUID: String, result: CHResult<CHEmpty>) {
        sendCommandSafely(
            SesameOS3Payload(
                SesameItemCode.SSM_OS3_PASSCODE_MOVE.value,
                byteArrayOf(cardId.hexStringToByteArray().size.toByte()) + cardId.hexStringToByteArray() + touchProUUID.toByteArray()
            ), result
        ) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun keyBoardPassCodeChange(ID: String, hexName: String, result: CHResult<CHEmpty>) {
        sendCommandSafely(
            SesameOS3Payload(SesameItemCode.SSM_OS3_PASSCODE_CHANGE.value, byteArrayOf(ID.hexStringToByteArray().size.toByte()) + ID.hexStringToByteArray() + hexName.chunked(2).map { it.toInt(16).toByte() }.toByteArray()),
            result
        ) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun sendKeyBoardPassCodeDataGetCmd(result: CHResult<CHEmpty>) {
        sendCommandSafely(SesameOS3Payload(SesameItemCode.SSM_OS3_PASSCODE_GET.value, byteArrayOf()), result) {}
    }

    override fun keyBoardPassCodeNameGet(keyBoardPassCode: String, keyBoardPassCodeNameUUID: String, subUUID: String, deviceUUID: String, result: CHResult<String>) {
        CHAccountManager.getKeyBoardPassCodeName(keyBoardPassCode, keyBoardPassCodeNameUUID, subUUID, deviceUUID) { it ->
            it.onSuccess {
                val keyBoardPassCodeName = it.data
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(keyBoardPassCodeName)))
            }
        }
    }

    override fun keyBoardPassCodeNameSet(keyBoardPassCodeNameRequest: CHKeyBoardPassCodeNameRequest, result: CHResult<String>) {
        CHAccountManager.setKeyBoardPassCodeName(keyBoardPassCodeNameRequest) { it ->
            it.onSuccess {
                val res = it.data
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            }
            it.onFailure { result.invoke(Result.failure(it)) }
        }
    }
    private val dataSyncCapable = CHDataSynchronizeCapableImpl()
    override fun getBoardPassCodeDataSyncCapable(): CHDataSynchronizeCapable {
        return dataSyncCapable
    }

    override fun registerEventDelegate(device: CHSesameBiometricBase, delegate: CHPassCodeDelegate) {
        setupSupport(device)
        val handler = CHPassCodeEventHandler(delegate)
        eventHandlers[delegate] = handler
        device.addEventHandler(handler)
    }

    override fun unregisterEventDelegate(device: CHSesameBiometricBase, delegate: CHPassCodeDelegate) {
        if (!isInitialized()) return
        eventHandlers[delegate]?.let { handler ->
            device.removeEventHandler(handler)
            eventHandlers.remove(delegate)
        }
    }
}