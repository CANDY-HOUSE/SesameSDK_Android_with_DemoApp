package co.candyhouse.sesame.ble.os3

import android.annotation.SuppressLint
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.CHadv
import co.candyhouse.sesame.ble.DeviceSegmentType
import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.Sesame2HistoryTypeEnum
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.SesameResultCode
import co.candyhouse.sesame.ble.isBleAvailable
import co.candyhouse.sesame.ble.os3.base.CHSesameOS3
import co.candyhouse.sesame.ble.os3.base.SesameOS3BleCipher
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.db.model.historyTagBLE
import co.candyhouse.sesame.db.model.historyTagIOT
import co.candyhouse.sesame.open.CHAccountManager
import co.candyhouse.sesame.open.CHAccountManager.makeApiCall
import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.open.CHResultState
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHSesame2MechStatus
import co.candyhouse.sesame.open.device.CHSesame5
import co.candyhouse.sesame.open.device.CHSesame5History
import co.candyhouse.sesame.open.device.CHSesame5MechSettings
import co.candyhouse.sesame.open.device.CHSesame5MechStatus
import co.candyhouse.sesame.open.device.CHSesame5OpsSettings
import co.candyhouse.sesame.open.device.NSError
import co.candyhouse.sesame.open.isInternetAvailable
import co.candyhouse.sesame.server.CHIotManager
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.server.dto.CHOS3RegisterReq
import co.candyhouse.sesame.utils.EccKey
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame.utils.base64decodeByteArray
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.toBigLong
import co.candyhouse.sesame.utils.toHexString
import co.candyhouse.sesame.utils.toReverseBytes
import co.candyhouse.sesame.utils.toUInt32ByteArray
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.math.abs

@SuppressLint("MissingPermission")
internal class CHSesame5Device : CHSesameOS3(), CHSesame5, CHDeviceUtil {
    private var currentDeviceUUID: UUID? = null
    private lateinit var HistoryTagWithUUID: ByteArray

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

    override var opsSetting: CHSesame5OpsSettings? = null

    /** 設備廣播: 不同設備廣播可以帶不同傳輸訊息 */
    override var advertisement: CHadv? = null
        set(value) {
            field = value
            parceADV(value)
            value?.let {
                /** 保留廣播通訊 */
//                isHistory = it.adv_tag_b1
                if (it.deviceID == currentDeviceUUID) {
                    isHistory = it.adv_tag_b1
                }
            }
        }


    /** 聯網處理 */
    var isConnectedByWM2: Boolean = false
    override fun goIOT() {
        L.d("hcia", "[ss5]goIOT:" + deviceId)
        CHIotManager.subscribeSesame2Shadow(this) { result ->
            result.onSuccess { resource ->
                L.d("hcia", "[ss5][iot]")
                L.d("hcia", "\uD83E\uDD5D [ss5]ss5_shadow裡存的hub3列表:" + resource.data.state.reported.wm2s)
                resource.data.state.reported.wm2s?.let { wm2s ->
                    L.d("hcia", "[ss5]wm2s:" + wm2s)
                    isConnectedByWM2 = wm2s.map { it.value.hexStringToByteArray().first().toInt() }.contains(1)
                }

                if (isConnectedByWM2) {
                    resource.data.state.reported.mechst?.let { mechShadow ->
                        // 【ID1001300】【Android】【app】Android app 蓝牙与 AWSIoT 都有设备状态时, 优先显示蓝牙状态（有时 AWSIoT 的角度开关锁状态是错的, 期望 UI 优先显示 Bluetooth的角度开关锁状态）
                        val res: CHResult<CHEmpty> = { }
                        if (!isBleAvailable(res)) { // 蓝牙不可用， 使用 iot 的状态。
                            mechStatus = CHSesame5MechStatus(CHSesame2MechStatus(mechShadow.hexStringToByteArray()).ss5Adapter())
                        }
                    }
                    deviceShadowStatus = if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
                } else {
                    deviceShadowStatus = null
                }

            }
        }

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
        if (!isBleAvailable(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.autolock.value, delay.toShort().toReverseBytes()), DeviceSegmentType.cipher) { res ->
            mechSetting?.autoLockSecond = delay.toShort()
            result.invoke(Result.success(CHResultState.CHResultStateBLE(delay)))
        }
    }

    override fun opSensorControl(isEnable: Int, result: CHResult<Int>) {
        if (!isBleAvailable(result)) return
        sendCommand(SesameOS3Payload(SesameItemCode.OPS_CONTROL.value, isEnable.toShort().toReverseBytes()), DeviceSegmentType.cipher) { res ->
            opsSetting?.opsLockSecond = isEnable.toUShort()
            result.invoke(Result.success(CHResultState.CHResultStateBLE(isEnable)))
        }
    }

    override fun magnet(result: CHResult<CHEmpty>) {
        if (!isBleAvailable(result)) return
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
    override fun history(cursor: Long?, uuid: UUID, subUUID: String?, result: CHResult<Pair<List<CHSesame5History>, Long?>>) {
        currentDeviceUUID = uuid
        historyCallback = result
        CHAccountManager.getHistory(this, cursor, subUUID) {

            L.d("historyType", "uuid:${uuid}")
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

                L.d("historyType", "uuid:${uuid} fial")
                L.d("hcia", "it:" + it)
                result.invoke(Result.failure(it))
            }
        } // end getHistory
    }

    override fun toggle(historytag: ByteArray?, result: CHResult<CHEmpty>) {
        if (deviceStatus.value == CHDeviceLoginStatus.Login && isBleAvailable(result)) {
            if (deviceStatus == CHDeviceStatus.Locked) {
                unlock(historytag, result)
            } else {
                lock(historytag, result)
            }
        } else {
            sesame2KeyData?.apply {
                CHAccountManager.cmdSesame(SesameItemCode.toggle, this@CHSesame5Device, this.historyTagIOT(historytag), result)
            }
        }
    }

    private fun uuidToByteArray(uuid: UUID): ByteArray {
        val bb = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return bb.array()
    }

    private fun buildHistoryTagWithUUID(): ByteArray? {
        // 将UUID转换为ByteArray
        val uuidByteArray = deviceId?.let { uuidToByteArray(it) }
        HistoryTagWithUUID = ByteArrayOutputStream().apply {
            write(uuidByteArray)
        }.toByteArray()
        L.d("history", HistoryTagWithUUID.toHexString())
        return HistoryTagWithUUID
    }

    override fun unlock(historytag: ByteArray?, result: CHResult<CHEmpty>) {
        if (historytag == null) {
            buildHistoryTagWithUUID()
        }
        if (deviceStatus.value == CHDeviceLoginStatus.UnLogin && deviceShadowStatus != null) {
            CHAccountManager.cmdSesame(SesameItemCode.unlock, this, sesame2KeyData!!.historyTagIOT(historytag), result)
            return
        }
        if (!isBleAvailable(result)) {
            CHAccountManager.cmdSesame(SesameItemCode.toggle, this, sesame2KeyData!!.historyTagIOT(historytag), result)
            return
        }
        sendCommand(SesameOS3Payload(SesameItemCode.unlock.value, sesame2KeyData!!.historyTagBLE(historytag)), DeviceSegmentType.cipher) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }

    override fun lock(historytag: ByteArray?, result: CHResult<CHEmpty>) {
        if (historytag == null) {
            buildHistoryTagWithUUID()
        }
        if (deviceStatus.value == CHDeviceLoginStatus.UnLogin && deviceShadowStatus != null) {
            CHAccountManager.cmdSesame(SesameItemCode.lock, this, sesame2KeyData!!.historyTagIOT(historytag), result)
            return
        }
        if (!isBleAvailable(result)) {
            CHAccountManager.cmdSesame(SesameItemCode.toggle, this, sesame2KeyData!!.historyTagIOT(historytag), result)
            return
        }
        sendCommand(SesameOS3Payload(SesameItemCode.lock.value, sesame2KeyData!!.historyTagBLE(historytag)), DeviceSegmentType.cipher) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
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
            CHAccountManager.jpAPIclient.myDevicesRegisterSesame5Post(deviceId.toString(), CHOS3RegisterReq(advertisement!!.productModel!!.productType().toString(), serverSecret))
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
        cipher = SesameOS3BleCipher("customDeviceName", sessionAuth!!, ("00" + mSesameToken.toHexString()).hexStringToByteArray())
        sendCommand(SesameOS3Payload(SesameItemCode.login.value, sessionAuth!!.sliceArray(0..3)), DeviceSegmentType.plain) { loginPayload ->
            val systemTime = loginPayload.payload.sliceArray(0..3).toBigLong()
//            L.d("hcia", "[ss5][ts]:" + systemTime)//1670037181
            val currentTimestamp = System.currentTimeMillis() / 1000
            val timeMinus = currentTimestamp.minus(systemTime)

//            L.d("hcia", "[ss5][login][timeMinus:$timeMinus]")


            if (abs(timeMinus) > 3) {
                L.d("hcia", "[ss5][login][timeMinus:$timeMinus]!!")
                sendCommand(SesameOS3Payload(SesameItemCode.time.value, System.currentTimeMillis().toUInt32ByteArray()), DeviceSegmentType.cipher) {}
            }
        }
    }

    private var isReadHistoryCommandRunning: Boolean = false
    private fun readHistoryCommand() {
        if (isReadHistoryCommandRunning) {
            L.d("hcia", "[ss5][his][read] readHistoryCommand is already running")
            return
        }
        val isConnectNET = isInternetAvailable()
        sendCommand(SesameOS3Payload(SesameItemCode.history.value, byteArrayOf(0x01)), DeviceSegmentType.cipher) { res -> // 01: 从设备读取最旧的历史记录
            L.d("hcia", "[ss5][his][ResultCode]:" + res.cmdResultCode)
            val hisPaylaod = res.payload
            if (res.cmdResultCode == SesameResultCode.success.value) {
                // 改为 uuid 格式的 hisTag， APP不再兼容旧固件的历史记录， 若有客诉历史记录问题， 请升级锁的固件。
                if (isConnectNET && !isConnectedByWM2) {
                    CHAccountManager.postSS5History(deviceId.toString().uppercase(), hisPaylaod.toHexString()) {
                        // 成功上传历史记录到云端后， 通过蓝牙删除这条历史记录， SS5固件会在它的Flash里删除掉这条历史记录。
                        val recordId = hisPaylaod.sliceArray(0..3)
                        it.onSuccess {
                            L.d("hcia", "[+]SSM2_ITEM_CODE_HISTORY_DELETE: ${recordId.toBigLong().toInt()}")
                            sendCommand(SesameOS3Payload(SesameItemCode.SSM2_ITEM_CODE_HISTORY_DELETE.value, recordId), DeviceSegmentType.cipher) { res ->
                                L.d("hcia", "[-]SSM2_ITEM_CODE_HISTORY_DELETE: ${res.cmdResultCode}")
                            }
                        }
                        it.onFailure { exception ->
                            L.d("hcia", "[ss5][history]postSS5History: $exception")
                        }
                    }
                }
            }
            isReadHistoryCommandRunning = false
        }
    }

    // 如果这台锁当前没有被 Hub3 连上云端， 则通过 APP 报告给云端。
    private fun reportBatteryData(payloadString: String) {
        L.d("harry", "[ss5][reportBatteryData]:" + isInternetAvailable() + ", " + !isConnectedByWM2 + ", payload: " + payloadString)
        if (isInternetAvailable() && !isConnectedByWM2) {
            CHAccountManager.postBatteryData(deviceId.toString().uppercase(), payloadString) {}
        }
    }

    /** 指令接收 */

    override fun onGattSesamePublish(receivePayload: SSM3PublishPayload) {
        super.onGattSesamePublish(receivePayload)
        L.d("onGattSesamePublish", "[ss5] " + receivePayload.cmdItCode + ", data: " + receivePayload.payload.toHexString())
        if (receivePayload.cmdItCode == SesameItemCode.SSM3_ITEM_CODE_BATTERY_VOLTAGE.value) {
            reportBatteryData(receivePayload.payload.toHexString())
        }
        if (receivePayload.cmdItCode == SesameItemCode.mechStatus.value) {
            mechStatus = CHSesame5MechStatus(receivePayload.payload)
            deviceStatus = if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked

//            L.d("readHistoryCommand", "mechStatus " + "${deviceStatus}")
//            readHistoryCommand()
        }
        if (receivePayload.cmdItCode == SesameItemCode.mechSetting.value) {
            mechSetting = CHSesame5MechSettings(receivePayload.payload)
        }
        if (receivePayload.cmdItCode == SesameItemCode.OPS_CONTROL.value) {
            opsSetting = CHSesame5OpsSettings(receivePayload.payload)
            L.d("switch", "[ss5][opsSecond]:" + opsSetting!!.opsLockSecond)
        }
    }

}