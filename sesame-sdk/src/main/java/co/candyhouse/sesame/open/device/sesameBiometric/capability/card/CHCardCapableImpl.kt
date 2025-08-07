package co.candyhouse.sesame.open.device.sesameBiometric.capability.card

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
import co.candyhouse.sesame.server.dto.CHCardNameRequest
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.padEnd
import co.candyhouse.sesame.utils.toHexString
import co.candyhouse.sesame.utils.toReverseBytes
import java.lang.Thread.sleep
import java.util.concurrent.CountDownLatch
import kotlin.text.chunked

internal open class CHCardCapableImpl() :
    CHAutoInitCapabilityImpl(),
    CHCardCapable {


    private val eventHandlers = mutableMapOf<CHCardDelegate, CHCardEventHandler>()


    override fun sendNfcCardsDataGetCmd(deviceUUID: String, result: CHResult<String>) {
        sendCommandSafely(
            SesameOS3Payload(SesameItemCode.SSM_OS3_CARD_GET.value, byteArrayOf()),
            result
        ) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE("BLE")))
        }
    }

    override fun cardModeGet(result: CHResult<Byte>) {
        sendCommandSafely(
            SesameOS3Payload(
                SesameItemCode.SSM_OS3_CARD_MODE_GET.value,
                byteArrayOf()
            ), result
        ) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(res.payload[0])))
        }
    }

    override fun cardModeSet(mode: Byte, result: CHResult<CHEmpty>) {
        sendCommandSafely(
            SesameOS3Payload(
                SesameItemCode.SSM_OS3_CARD_MODE_SET.value,
                byteArrayOf(mode)
            ), result
        ) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun cardDelete(cardID: String, result: CHResult<CHEmpty>) {
        sendCommandSafely(
            SesameOS3Payload(
                SesameItemCode.SSM_OS3_CARD_DELETE.value,
                cardID.hexStringToByteArray()
            ), result
        ) { res ->
            L.d("hcia", "[cardDelete][ID]" + cardID)
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun cardMove(cardId: String, touchProUUID: String, result: CHResult<CHEmpty>) {
        sendCommandSafely(
            SesameOS3Payload(
                SesameItemCode.SSM_OS3_CARD_MOVE.value,
                byteArrayOf(cardId.hexStringToByteArray().size.toByte()) + cardId.hexStringToByteArray() + touchProUUID.toByteArray()
            ), result
        ) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun cardAdd(id: ByteArray, name: String, result: CHResult<CHEmpty>) {
        sendCommandSafely(
            SesameOS3Payload(
                SesameItemCode.SSM_OS3_CARD_ADD.value,
                byteArrayOf(0xF0/*CARD_DATA_USED*/.toByte())+ byteArrayOf(0x80/*CARD_TYPE_CLOUD_BASE*/.toByte()) + byteArrayOf(id.size.toByte()) + id.padEnd(16, 0x00.toByte()) + byteArrayOf(name.toByteArray().size.toByte()) + name.toByteArray().padEnd(16, 0x00.toByte())
            ), result
        ) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun cardBatchAdd(id: ByteArray, progressCallback: ((current: Int, total: Int) -> Unit)?, result: CHResult<CHEmpty>) {
        Thread {
            try {
                val dataSize: Short = id.size.toShort()
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

                    tempList.addAll(id.slice(dataIndex until (dataIndex + chunkSize)))
                    dataIndex = (dataIndex + chunkSize).toShort()

                    val batchData = tempList.toByteArray()
                    L.d("sf", "Packet $currentPacket/$totalPackets - size: ${batchData.size}")

                    val latch = CountDownLatch(1)
                    var sendSuccess = false

                    sendCommandSafely(
                        SesameOS3Payload(StpItemCode.STP_ITEM_CODE_CARDS_ADD.value, batchData),
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

    // 新方案：设备接收 16位的uuid 作为 name
    override fun cardChange(ID: String, name: String, result: CHResult<CHEmpty>) {
        sendCommandSafely(
            SesameOS3Payload(
                SesameItemCode.SSM_OS3_CARD_CHANGE.value,
                byteArrayOf(ID.hexStringToByteArray().size.toByte()) + ID.hexStringToByteArray() + name.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            ), result
        ) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun cardChangeValue(ID: String, newID: String, result: CHResult<CHEmpty>) {
        sendCommandSafely(
            SesameOS3Payload(
                SesameItemCode.SSM_OS3_CARD_CHANGE_VALUE.value,
                byteArrayOf(ID.hexStringToByteArray().size.toByte()) + ID.hexStringToByteArray() + newID.toByteArray()
            ), result
        ) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    override fun cardNameGet(cardID: String, cardNameUUID: String, subUUID: String, deviceUUID: String, result: CHResult<String>) {
        CHAccountManager.getCardName(cardID, cardNameUUID, subUUID, deviceUUID) { it ->
            it.onSuccess {
                val cardName = it.data
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(cardName)))
            }
        }
    }

    override fun cardNameSet(cardNameRequest: CHCardNameRequest, result: CHResult<String>) {
        CHAccountManager.setCardName(cardNameRequest) { it ->
            it.onSuccess {
                val res = it.data
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            }
            it.onFailure { result.invoke(Result.failure(it)) }
        }
    }

    private val dataSyncCapable = CHDataSynchronizeCapableImpl()
    override fun getCardDataSyncCapable(): CHDataSynchronizeCapable {
        return dataSyncCapable
    }

    override fun registerEventDelegate(device: CHSesameBiometricBase, delegate: CHCardDelegate) {
        setupSupport(device)
        val handler = CHCardEventHandler(delegate)
        eventHandlers[delegate] = handler
        device.addEventHandler(handler)
    }

    override fun unregisterEventDelegate(device: CHSesameBiometricBase, delegate: CHCardDelegate) {
        if (!isInitialized()) return
        eventHandlers[delegate]?.let { handler ->
            device.removeEventHandler(handler)
            eventHandlers.remove(delegate)
        }
    }
}