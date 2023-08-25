package co.candyhouse.sesame.ble.os3

import android.annotation.SuppressLint
import android.bluetooth.*
import android.preference.PreferenceManager
import co.candyhouse.sesame.ble.*
import co.candyhouse.sesame.ble.os3.base.CHSesameOS3
import co.candyhouse.sesame.ble.os3.base.SesameOS3BleCipher
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.db.model.createHistagV2
import co.candyhouse.sesame.db.model.hisTagC
import co.candyhouse.sesame.open.*
import co.candyhouse.sesame.open.CHAccountManager.makeApiCall
import co.candyhouse.sesame.open.device.*
import co.candyhouse.sesame.server.dto.*
import co.candyhouse.sesame.utils.*
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame2.BuildConfig
import java.util.*
import kotlin.math.abs

@SuppressLint("MissingPermission") internal class CHSesame5Device : CHSesameOS3(), CHSesame5, CHDeviceUtil {
    private var currentDeviceUUID: UUID? = null
    /** 其他功能: history  */
    private var historyCallback: CHResult<Pair<List<CHSesame5History>, Long?>>? = null
    var isHistory: Boolean = false
        set(value) {
            if (deviceStatus.value == CHDeviceLoginStatus.Login) {
//                if (field != value) {
                    field = value
//                    L.d("hcia", "[ss5] isHistory!!:" + isHistory)
                    if (field) {
                        readHistoryCommand()
                    }
//                }
            }
        }

    /** 設備設定 */
    override var mechSetting: CHSesame5MechSettings? = null

    /** 設備廣播: 不同設備廣播可以帶不同傳輸訊息 */
    override var advertisement: CHadv? = null
        set(value) {
            field = value
            parceADV(value)
            value?.let {
                /** 保留廣播通訊 */
//                isHistory = it.adv_tag_b1
                if(it.deviceID == currentDeviceUUID){
                    isHistory = it.adv_tag_b1
                }
            }
        }


    /** 聯網處理 */
    var isConnectedByWM2: Boolean = false
    override fun goIOT() {


    }

    /** 指令發送 */
    override fun configureLockPosition(lockTarget: Short, unlockTarget: Short, result: CHResult<CHEmpty>) {
        val cmd = SesameOS3Payload(SesameItemCode.mechSetting.value, lockTarget.toReverseBytes() + unlockTarget.toReverseBytes())
        sendCommand(cmd, DeviceSegmentType.cipher) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                mechSetting?.lockPosition = lockTarget
                mechSetting?.unlockPosition = unlockTarget
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }

    override fun autolock(delay: Int, result: CHResult<Int>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.autolock.value, delay.toShort().toReverseBytes()), DeviceSegmentType.cipher) { res ->
            mechSetting?.autoLockSecond = delay.toShort()
            result.invoke(Result.success(CHResultState.CHResultStateBLE(delay)))
        }
    }

    override fun magnet(result: CHResult<CHEmpty>) {
        if (checkBle(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.magnet.value, byteArrayOf()), DeviceSegmentType.cipher) { res ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }

    private fun eventToHistory(historyType: Sesame2HistoryTypeEnum?, ts: Long, recordID: Int, mechStatus: CHSesame5MechStatus?, histag: ByteArray?): CHSesame5History? {
        return when (historyType) {
            Sesame2HistoryTypeEnum.MANUAL_UNLOCKED -> CHSesame5History.ManualUnlocked(ts, recordID, mechStatus, null)
            Sesame2HistoryTypeEnum.MANUAL_LOCKED -> CHSesame5History.ManualLocked(ts, recordID, mechStatus, null)
            Sesame2HistoryTypeEnum.BLE_LOCK -> CHSesame5History.BLELock(ts, recordID, mechStatus, histag)
            Sesame2HistoryTypeEnum.BLE_UNLOCK -> CHSesame5History.BLEUnlock(ts, recordID, mechStatus, histag)
            Sesame2HistoryTypeEnum.AUTOLOCK -> CHSesame5History.AutoLock(ts, recordID, mechStatus, null)
            Sesame2HistoryTypeEnum.WM2_LOCK -> CHSesame5History.WM2Lock(ts, recordID, mechStatus, histag)
            Sesame2HistoryTypeEnum.WM2_UNLOCK -> CHSesame5History.WM2Unlock(ts, recordID, mechStatus, histag)
            Sesame2HistoryTypeEnum.WEB_LOCK -> CHSesame5History.WEBLock(ts, recordID, mechStatus, histag)
            Sesame2HistoryTypeEnum.WEB_UNLOCK -> CHSesame5History.WEBUnlock(ts, recordID, mechStatus, histag)
            else -> null
        }
    }

//    override fun history(cursor: Long?, result: CHResult<Pair<List<CHSesame5History>, Long?>>) {
    override fun history(cursor: Long?, uuid: UUID, result: CHResult<Pair<List<CHSesame5History>, Long?>>) {
        currentDeviceUUID = uuid
        historyCallback = result

        CHAccountManager.getHistory(this, cursor) {
            it.onSuccess {
                val chHistorysToUI = ArrayList<CHSesame5History>()
//                L.d("hcia", "it.data:" + it.data)
                it.data.histories.forEach {
                    val historyType = Sesame2HistoryTypeEnum.getByValue(it.type)
                    val ts = it.timeStamp
                    val recordID = it.recordID
                    val histag = it.historyTag?.base64decodeByteArray()
                    val params = it.parameter?.base64decodeByteArray()
                    var mechStatus: CHSesame5MechStatus? = null
                    if (params != null) {
                        mechStatus = CHSesame5MechStatus(params)
                    }

                    val tmphis = eventToHistory(historyType, ts, recordID, mechStatus, histag)
                    if (tmphis != null) {
                        chHistorysToUI.add(tmphis)
                    }
                }
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(Pair(chHistorysToUI.toList(), it.data.cursor))))
            }
            it.onFailure {
                L.d("hcia", "it:" + it)
                result.invoke(Result.failure(it))
            }
        } //end getHistory
    }

    override fun toggle(historytag: ByteArray?, result: CHResult<CHEmpty>) {
        if (deviceStatus.value == CHDeviceLoginStatus.UnLogin && isConnectedByWM2) {
            CHAccountManager.cmdSesame(SesameItemCode.toggle, this, sesame2KeyData!!.hisTagC(historytag), result)
        } else {
            if (deviceStatus == CHDeviceStatus.Locked) {
                unlock(historytag, result)
            } else {
                lock(historytag, result)
            }
        }
    }

    override fun unlock(historytag: ByteArray?, result: CHResult<CHEmpty>) {
        if (deviceStatus.value == CHDeviceLoginStatus.UnLogin && isConnectedByWM2) {
            CHAccountManager.cmdSesame(SesameItemCode.unlock, this, sesame2KeyData!!.hisTagC(historytag), result)
        } else {
            if (checkBle(result)) return
//        L.d("hcia", "[ss5][unlock] historyTag:" + sesame2KeyData!!.createHistagV2(historyTag).toHexString())
            sendCommand(SesameOS3Payload(SesameItemCode.unlock.value, sesame2KeyData!!.createHistagV2(historytag)), DeviceSegmentType.cipher) { res ->
                if (res.cmdResultCode == SesameResultCode.success.value) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                } else {
                    result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
                }
            }
        }
    }

    override fun lock(historytag: ByteArray?, result: CHResult<CHEmpty>) {
        if (deviceStatus.value == CHDeviceLoginStatus.UnLogin && isConnectedByWM2) {
            CHAccountManager.cmdSesame(SesameItemCode.lock, this, sesame2KeyData!!.hisTagC(historytag), result)
        } else {
            if (checkBle(result)) return
//        L.d("hcia", "[ss5][lock] historyTag:" + sesame2KeyData!!.createHistagV2(historyTag).toHexString())
            sendCommand(SesameOS3Payload(SesameItemCode.lock.value, sesame2KeyData!!.createHistagV2(historytag)), DeviceSegmentType.cipher) { res ->
                if (res.cmdResultCode == SesameResultCode.success.value) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                } else {
                    result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
                }
            }
        }

    }

    override fun register(result: CHResult<CHEmpty>) {
        if (deviceStatus != CHDeviceStatus.ReadyToRegister) {
            result.invoke(Result.failure(NSError("Busy", "CBCentralManager", 7)))
            return
        }
        deviceStatus = CHDeviceStatus.Registering

        L.d("hcia", "register:!!")
        makeApiCall(result) {
            val serverSecret = mSesameToken.toHexString()
            sendCommand(SesameOS3Payload(SesameItemCode.registration.value, EccKey.getPubK().hexStringToByteArray() + System.currentTimeMillis().toUInt32ByteArray()), DeviceSegmentType.plain) { IRRes ->
                mechStatus = CHSesame5MechStatus(IRRes.payload.toHexString().hexStringToByteArray().sliceArray(0..6))
                mechSetting = CHSesame5MechSettings(IRRes.payload.toHexString().hexStringToByteArray().sliceArray(7..12))

                val eccPublicKeyFromSS5 = IRRes.payload.toHexString().hexStringToByteArray().sliceArray(13..76)
                val ecdhSecret = EccKey.ecdh(eccPublicKeyFromSS5)
                val ecdhSecretPre16 = ecdhSecret.sliceArray(0..15)
                val deviceSecret = ecdhSecretPre16.toHexString()
                val candyDevice = CHDevice(deviceId.toString(), advertisement!!.productModel!!.deviceModel(), null, "0000", deviceSecret, serverSecret)
                sesame2KeyData = candyDevice

                val sessionAuth = AesCmac(ecdhSecretPre16, 16).computeMac(mSesameToken)

                cipher = SesameOS3BleCipher("customDeviceName", sessionAuth!!, ("00" + mSesameToken.toHexString()).hexStringToByteArray())
                CHDB.CHSS2Model.insert(candyDevice) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                }

                deviceStatus = if (mechStatus?.isInLockRange == true) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked

            }
        }
    }

    override fun login(token: String?) {
        deviceStatus = CHDeviceStatus.BleLogining
        val sessionAuth: ByteArray? = if (isNeedAuthFromServer == true) {
            token?.hexStringToByteArray()
        } else {
            AesCmac(sesame2KeyData!!.secretKey.hexStringToByteArray(), 16).computeMac(mSesameToken)
        }
        L.d("login","isNeedAuthFromServer:$isNeedAuthFromServer--${mSesameToken.toHexString()}--sessionAuth:${sessionAuth}---${sesame2KeyData!!.secretKey.hexStringToByteArray()}")


        cipher = SesameOS3BleCipher("customDeviceName", sessionAuth!!, ("00" + mSesameToken.toHexString()).hexStringToByteArray())
        sendCommand(SesameOS3Payload(SesameItemCode.login.value, sessionAuth!!.sliceArray(0..3)), DeviceSegmentType.plain) { loginPayload ->
            val systemTime = loginPayload.payload.sliceArray(0..3).toBigLong()
//            L.d("hcia", "[ss5][ts]:" + systemTime)//1670037181
            val currentTimestamp = System.currentTimeMillis() / 1000
            val timeMinus = currentTimestamp.minus(systemTime)

//            L.d("hcia", "[ss5][login][timeMinus:$timeMinus]")

            if (PreferenceManager.getDefaultSharedPreferences(CHBleManager.appContext).getString("nickname", "")?.contains(BuildConfig.testname) == true) {
                deviceTimestamp = systemTime
                loginTimestamp = currentTimestamp
            } else {
                if (abs(timeMinus) > 3) {
                    L.d("hcia", "[ss5][login][timeMinus:$timeMinus]!!")
                    sendCommand(SesameOS3Payload(SesameItemCode.time.value, System.currentTimeMillis().toUInt32ByteArray()), DeviceSegmentType.cipher) {}
                }
            }
        }
    }

    private fun readHistoryCommand() {
//        L.d("hcia", "[ss5][his][read] isHistory:" + isHistory)
        val isConnectNET = isInternetAvailable()
        sendCommand(SesameOS3Payload(SesameItemCode.history.value, if (isConnectNET) byteArrayOf(0x01) else byteArrayOf(0x00)), DeviceSegmentType.cipher) { res ->
//            L.d("hcia", "[ss5][his][ResultCode]:" + res.cmdResultCode)
            if (res.cmdResultCode == SesameResultCode.notFound.value) {
                isHistory = false
            }
            if (res.cmdResultCode == SesameResultCode.success.value) {

                val recordId = res.payload.sliceArray(0..3).toBigLong().toInt()
                var historyType = Sesame2HistoryTypeEnum.getByValue(res.payload[4]) ?: Sesame2HistoryTypeEnum.NONE
                val newTime = res.payload.sliceArray(5..8).toBigLong() //4
                val mechStatus = CHSesame5MechStatus(res.payload.sliceArray(9..15))
                var historyContent = res.payload.sliceArray(16..res.payload.count() - 1)

//                L.d("hcia", "historyType:" + historyType)
                if (historyType == Sesame2HistoryTypeEnum.BLE_LOCK) {
                    val tagcount = historyContent[0] % 30
                    val historyOpType = historyContent[0] / 30
                    if (historyOpType == 1) {
                        historyType = Sesame2HistoryTypeEnum.WM2_LOCK
                    }
                    if (historyOpType == 2) {
                        historyType = Sesame2HistoryTypeEnum.WEB_LOCK
                    }
                    historyContent[0] = tagcount.toByte()
                    historyContent = historyContent.toCutedHistag()!!
                }
                if (historyType == Sesame2HistoryTypeEnum.BLE_UNLOCK) {
                    val tagcount = historyContent[0] % 30
                    val historyOpType = historyContent[0] / 30
                    if (historyOpType == 1) {
                        historyType = Sesame2HistoryTypeEnum.WM2_UNLOCK
                    }
                    if (historyOpType == 2) {
                        historyType = Sesame2HistoryTypeEnum.WEB_UNLOCK
                    }
                    historyContent[0] = tagcount.toByte()
                    historyContent = historyContent.toCutedHistag()!!
                }

//                L.d("hcia", "historyType:" + historyType + " historyContent:" + historyContent.toHexString())
//                L.d("hcia", "[ss5][history]recordId:" + recordId + " historyType:" + historyType)
                val chHistorysToUI = listOf(eventToHistory(historyType, newTime * 1000, recordId, mechStatus, historyContent)!!)
                historyCallback?.invoke(Result.success(CHResultState.CHResultStateBLE(Pair(chHistorysToUI, null))))
                if (isConnectNET) {

                }

            }

        }
    }

    /** 指令接收 */

    override fun onGattSesamePublish(receivePayload: SSM3PublishPayload) {
        super.onGattSesamePublish(receivePayload)
//        L.d("hcia", "[ss5] " + receivePayload.cmdItCode)
        if (receivePayload.cmdItCode == SesameItemCode.mechStatus.value) {
            mechStatus = CHSesame5MechStatus(receivePayload.payload)
            deviceStatus = if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
            readHistoryCommand()
        }
        if (receivePayload.cmdItCode == SesameItemCode.mechSetting.value) {
            mechSetting = CHSesame5MechSettings(receivePayload.payload)
        }
    }

}
