package co.candyhouse.sesame.ble.os2

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.pm.PackageManager
import android.os.Build
import co.candyhouse.sesame.ble.*
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.SesameResultCode
import co.candyhouse.sesame.ble.SesameNotifypayload
import co.candyhouse.sesame.ble.SSM2OpCode
import co.candyhouse.sesame.ble.SSM2ResponsePayload
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.checkBle
import co.candyhouse.sesame.ble.SesameBleTransmit
import co.candyhouse.sesame.ble.os2.base.CHSesameOS2
import co.candyhouse.sesame.ble.os2.base.SSM2Payload
import co.candyhouse.sesame.ble.os2.base.SesameOS2BleCipher
import co.candyhouse.sesame.ble.os2.base.SesameOS2ResponseCallback
import co.candyhouse.sesame.open.CHBleManager.appContext
import co.candyhouse.sesame.open.CHBleManager.bluetoothAdapter
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.db.model.createHistag
import co.candyhouse.sesame.db.model.hisTagC
import co.candyhouse.sesame.open.*
import co.candyhouse.sesame.open.device.*
import co.candyhouse.sesame.open.CHAccountManager.makeApiCall
import co.candyhouse.sesame.server.dto.*
import co.candyhouse.sesame.server.dto.CHHistoryEvent
import co.candyhouse.sesame.utils.*
import co.candyhouse.sesame.utils.aescmac.AesCmac
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

internal enum class CHError(val value: NSError) {
    SesameUnlogin(NSError("Sesame BLE unlogin", "SesameSDK", -1)), Noble(NSError("without receivedBle", "SesameSDK", -2)), BlePoweroff(NSError("PoweredOff", "CBCentralManager", 4)), BleUnauth(NSError("Unauthorized", "CBCentralManager", 3)), BUSY(NSError("BUSY", "Sesame2SDK", 7)), INVALID_PARAM(NSError("INVALID_PARAM", "Sesame2SDK", 8)), BleInvalidAction(NSError("InvalidAction", "Sesame2SDK", 9)), NotfoundError(NSError("notfound", "Sesame2SDK", 5)), NetWorkError(NSError("NetWork", "Sesame2SDK", -3)),
}

@SuppressLint("MissingPermission") internal class CHSesame2Device() : CHSesameOS2(), CHSesame2, CHDeviceUtil {
    private var mResultRegister: CHResult<CHEmpty>? = null
    var isConnectedByWM2: Boolean = false

    private var historyCallback: CHResult<Pair<List<CHSesame2History>, Long?>>? = null

    override fun goIOT() {

    }

    override var advertisement: CHadv? = null
        set(value) {
            field = value
            if (value == null) { //                L.d("hcia", "[ssm] [adv] end:" + value)
                rssi = -100
                deviceStatus = CHDeviceStatus.NoBleSignal
                return
            }
            rssi = advertisement?.rssi
            deviceId = advertisement!!.deviceID
            isRegistered = advertisement!!.isRegistered
            productModel = advertisement!!.productModel!!

            if (deviceStatus.value == CHDeviceLoginStatus.Login && !isRegistered) {
                deviceStatus = CHDeviceStatus.Reset
            }

            if (deviceStatus == CHDeviceStatus.NoBleSignal) {
                deviceStatus = CHDeviceStatus.ReceivedAdV
            }

            if (deviceStatus == CHDeviceStatus.WaitingForAuth && isInternetAvailable()) {
                deviceStatus = CHDeviceStatus.ReceivedAdV
            }

        }


    override var mechSetting: CHSesame2MechSettings? = null


    override fun getVersionTag(result: CHResult<String>) {
        if (checkBle(result)) return
        sendEncryptCommand(SSM2Payload(SSM2OpCode.read, SesameItemCode.versionTag, byteArrayOf())) { res ->
            val gitTag = res.payload.sliceArray(4..15)
            CHAccountManager.putSesameInfor(this, String(gitTag)) {}
            result.invoke(Result.success(CHResultState.CHResultStateBLE(String(gitTag))))
        }
    }

    override fun enableAutolock(delay: Int, historytag: ByteArray?, result: CHResult<Int>) {
        if (checkBle(result)) return

        sendEncryptCommand(SSM2Payload(SSM2OpCode.update, SesameItemCode.autolock, delay.toShort().toReverseBytes() + sesame2KeyData!!.createHistag(historytag))) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(delay)))
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }

    override fun disableAutolock(historytag: ByteArray?, result: CHResult<Int>) {
        enableAutolock(0, historytag, result)
    }

    override fun getAutolockSetting(result: CHResult<Int>) {
        if (checkBle(result)) return

        sendEncryptCommand(SSM2Payload(SSM2OpCode.read, SesameItemCode.autolock, byteArrayOf())) { res ->
//            val autoLockSecond = res.payload.reversedArray().toInt()
            val autoLockSecond =  java.lang.Long.parseLong(res.payload.reversedArray().toHexString(), 16).toInt()
            result.invoke(Result.success(CHResultState.CHResultStateBLE(autoLockSecond)))
        }
    }


    override fun toggle(historytag: ByteArray?, result: CHResult<CHEmpty>) {
        if (deviceStatus.value == CHDeviceLoginStatus.UnLogin && isConnectedByWM2) {
            CHAccountManager.cmdSesame(SesameItemCode.toggle, this, sesame2KeyData!!.hisTagC(historytag), result)
        } else {
            if (mechStatus?.isInLockRange == true) {
                unlock(historytag, result)
            } else {
                lock(historytag, result)
            }
        }
    }

    override fun lock(historytag: ByteArray?, result: CHResult<CHEmpty>) {
        if (deviceStatus.value == CHDeviceLoginStatus.UnLogin && isConnectedByWM2) {
            CHAccountManager.cmdSesame(SesameItemCode.lock, this, sesame2KeyData!!.hisTagC(historytag), result)
        } else {
            if (checkBle(result)) return
            sendEncryptCommand(SSM2Payload(SSM2OpCode.async, SesameItemCode.lock, sesame2KeyData!!.createHistag(historytag))) { res ->
                if (res.cmdResultCode == SesameResultCode.success.value) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                } else {
                    result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
                }
            }
        }
    }

    override fun unlock(historytag: ByteArray?, result: CHResult<CHEmpty>) {

        if (deviceStatus.value == CHDeviceLoginStatus.UnLogin && isConnectedByWM2) {
            CHAccountManager.cmdSesame(SesameItemCode.unlock, this, sesame2KeyData!!.hisTagC(historytag), result)
        } else {
            if (checkBle(result)) return
            val his = sesame2KeyData!!.createHistag(historytag)
            sendEncryptCommand(SSM2Payload(SSM2OpCode.async, SesameItemCode.unlock, his)) { res ->
                if (res.cmdResultCode == SesameResultCode.success.value) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                } else {
                    result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
                }
            }
        }

    }

    override fun login(token: String?) { //        L.d("hcia", "loginSesame BluetoothGatt " + " mSSTK:" + mSesameToken.toHexString())
        //        L.d("hcia", "semaphore.isEmpty:" + semaphore.isEmpty)
//        L.d("hcia", "[ss2][login]")
//        val ssm = this
//        L.d("hcia", "ssm.sesame2KeyData!!.deviceUUID:" + ssm.sesame2KeyData!!.deviceUUID.uppercase())
//        val noHashUUID = ssm.sesame2KeyData!!.deviceUUID.replace("-", "")
//        val b64k = noHashUUID.hexStringToByteArray().base64Encode().replace("=", "")
//        val ssmIRData = b64k.toByteArray()
//        val ssmPKData = ssm.sesame2KeyData!!.sesame2PublicKey.hexStringToByteArray()
//        val ssmSecKa = ssm.sesame2KeyData!!.secretKey.hexStringToByteArray()
//        val ssmUUid = ssm.sesame2KeyData!!.deviceUUID.uppercase(Locale.getDefault()).toByteArray()
//        val allKey = ssmIRData + ssmPKData + ssmSecKa + ssmUUid
//        L.d("hcia", "[TEST][mSesameToken]:" + mSesameToken.toHexString()+" "+mSesameToken.size)
//        L.d("hcia", "[TEST][IR]:" + ssmIRData.toHexString())
//        L.d("hcia", "[TEST][ssm-public-key]:" + ssmPKData.toHexString())
//        L.d("hcia", "[TEST][ssm-secret-key]:" + ssmSecKa.toHexString())
//        L.d("hcia", "[TEST][ALL]:" + allKey.toHexString())

        semaphore = Channel(capacity = 1)
        deviceStatus = CHDeviceStatus.BleLogining
        val secret = sesame2KeyData!!.secretKey.hexStringToByteArray()
        val userIdx = sesame2KeyData!!.keyIndex.hexStringToByteArray()
        val ssmPublicKeyBytes = sesame2KeyData!!.sesame2PublicKey.hexStringToByteArray()
        val appPublicKeyBytes = EccKey.getPubK().hexStringToByteArray()
        val sessionToken = mAppToken + mSesameToken
        val signPayload = userIdx + appPublicKeyBytes + sessionToken
        val sessionAuth: ByteArray?
        if (isNeedAuthFromServer == true) {
            sessionAuth = token?.hexStringToByteArray()
        } else {
            sessionAuth = AesCmac(secret, 16).computeMac(signPayload)
        }

        val ecdhSecret = EccKey.ecdh(ssmPublicKeyBytes)
        val ecdhSecretPre16 = ecdhSecret.sliceArray(0..15)
        val sessionKey = AesCmac(ecdhSecretPre16, 16).computeMac(sessionToken)
//        L.d("hcia", "ecdhSecretPre16:" + ecdhSecretPre16.toHexString())
//        L.d("hcia", "sessionKey:" + sessionKey!!.toHexString())
        cipher = SesameOS2BleCipher(sessionKey!!, sessionToken)
        val loginPayload = userIdx + appPublicKeyBytes + mAppToken + sessionAuth!!.sliceArray(0..3)
//        L.d("hcia", "loginPayload:"+loginPayload.toHexString()+" " + loginPayload.size)
        val cmd = SSM2Payload(SSM2OpCode.sync, SesameItemCode.login, loginPayload) //        L.d("hcia", "下指令登入成功---->")
        sendEncryptCommand(cmd, DeviceSegmentType.plain) { ssm2ResponsePayload -> //            L.d("hcia", "下指令登入成功<-------")
            if (ssm2ResponsePayload.cmdItCode == SesameItemCode.login.value && ssm2ResponsePayload.cmdResultCode == SesameResultCode.success.value) {
//                L.d("hcia", "ssm2ResponsePayload.payload:" + ssm2ResponsePayload.payload.toHexString())
                val loginResponse = SSM2LoginResponsePayload(ssm2ResponsePayload.payload)
                val currentTimestamp = System.currentTimeMillis() / 1000
                val timeError = currentTimestamp.minus(loginResponse.systemTime)
                if (abs(timeError) > 3) {
                    if (loginResponse.fw_version >= 1) {
                        sendEncryptCommand(SSM2Payload(SSM2OpCode.update, SesameItemCode.timePhone, System.currentTimeMillis().toUInt32ByteArray())) { }
                    }
                }
                mechStatus = loginResponse.SSM2MechStatus
                mechSetting = loginResponse.SSM2MechSetting
                deviceStatus = if (!mechSetting!!.isConfigured) CHDeviceStatus.NoSettings else if ((mechStatus as CHSesame2MechStatus).isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked

            }
        }
    }


    override fun connect(result: CHResult<CHEmpty>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (CHBleManager.mScanning == CHScanStatus.BleClose) {
                    result.invoke(Result.failure(CHError.BleUnauth.value))
                    return
                }
            }
        }

        if (CHBleManager.mScanning == CHScanStatus.BleClose) {
            result.invoke(Result.failure(CHError.BlePoweroff.value))
            return
        }
        if (deviceStatus != CHDeviceStatus.ReceivedAdV) {
            result.invoke(Result.failure(CHError.Noble.value))
            return
        }

        if (isNeedAuthFromServer == true && isInternetAvailable() == false) {
            deviceStatus = CHDeviceStatus.WaitingForAuth
            return
        }


        if (CHBleManager.connectR.indexOf(advertisement?.device?.address) == -1) { //            L.b("hcia", customDeviceName + ":連接紀錄:" + advertisement!!.device.address)
            CHBleManager.connectR.add(advertisement!!.device.address)
        } else { //            L.b("hcia", customDeviceName + ":我已經連接過了：" + (advertisement as CHadv).device.address)
            return
        }

        deviceStatus = CHDeviceStatus.BleConnecting
        result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))

        //        L.d("hcia", "Build.VERSION.SDK_INT:" + Build.VERSION.SDK_INT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //                L.d("hcia", "ssm" + ":連接 O:")
            bluetoothAdapter.getRemoteDevice(advertisement!!.device.address).connectGatt(appContext, false, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //                L.d("hcia", "ssm" + ":連接 M:")
            bluetoothAdapter.getRemoteDevice(advertisement!!.device.address).connectGatt(appContext, false, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE)
        } else { //                L.d("hcia", "ssm" + ":主動連接 old:")
            bluetoothAdapter.getRemoteDevice(advertisement!!.device.address).connectGatt(appContext, false, mBluetoothGattCallback)
        }
    }

    private val mBluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
//            L.d("hcia", "[ssm][say]:" + characteristic!!.value.toHexString())
            val ssmSay = gattRxBuffer.feed(characteristic!!.value)

            if (ssmSay?.first == DeviceSegmentType.cipher) {
                parceNotifyPayload(cipher!!.decrypt(ssmSay.second))
            }
            if (ssmSay?.first == DeviceSegmentType.plain) {
                parceNotifyPayload(ssmSay.second)
            }
        }

        private fun parceNotifyPayload(palntext: ByteArray) {
//            L.d("hcia", "palntext:" + palntext.toHexString())
            val ssm2notify = SesameNotifypayload(palntext) //1
            if (ssm2notify.notifyOpCode == SSM2OpCode.publish) {
                val ssm2pubPayload = SSM3PublishPayload(ssm2notify.payload)
                onGattSesamePublish(ssm2pubPayload)
            }
            if (ssm2notify.notifyOpCode == SSM2OpCode.response) {
                val ssm2responsePayload = SSM2ResponsePayload(ssm2notify.payload)
//                L.d("hcia", "🀄Command:<==:" + ssm2responsePayload.cmdOPCode + "-" + ssm2responsePayload.cmdItCode + "-" + ssm2responsePayload.cmdResultCode)
                CoroutineScope(IO).launch {
                    semaphore.receive().invoke(ssm2responsePayload)
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            transmit()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
//            L.d("hcia", "BluetoothGatt " + ":發現服務:")
            for (service in gatt?.services!!) {
//                L.d("hcia", "service.uuid:" + service.uuid)
                if (service.uuid == Sesame2Chracs.uuidService01) {
                    for (charc in service.characteristics) {
//                        L.d("hcia", "BluetoothGatt 特徵:" + charc.uuid)
//                        L.d("hcia", "charc.uuid:" + charc.uuid)
                        if (charc.uuid == Sesame2Chracs.uuidChr02) {
                            mCharacteristic = charc
                        }

                        if (charc.uuid == Sesame2Chracs.uuidChr03) {
                            gatt.setCharacteristicNotification(charc, true)
                            val descriptor = charc.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                            gatt.writeDescriptor(descriptor)
                        }
                    }
                }
            }
        }


        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) { //                L.d("hcia", "ssm " + ":連接狀態＋＋" + BleBaseType.GattConnectStateDec(newState) + " 狀態:" + BleBaseType.GattConnectStatusDec(status))
                deviceStatus = CHDeviceStatus.DiscoverServices
                mBluetoothGatt = gatt
                gatt.discoverServices()
            } else {
//                L.d("hcia", "ssm " + ":連接狀態 --" + BleBaseType.GattConnectStateDec(newState) + " 狀態:" + BleBaseType.GattConnectStatusDec(status))
                gatt.close()
                advertisement = null
                mBluetoothGatt = null
                CHBleManager.connectR.remove(gatt.device.address)
            }
        }
    }


    override fun register(resultRegister: CHResult<CHEmpty>) {

        if (deviceStatus != CHDeviceStatus.ReadyToRegister) {
            resultRegister.invoke(Result.failure(CHError.BUSY.value))
            return
        }


        semaphore = Channel(capacity = 1)

        sendEncryptCommand(SSM2Payload(SSM2OpCode.read, SesameItemCode.IRER, byteArrayOf()), DeviceSegmentType.plain) { IRRes ->
            L.d("hcia", "IRER:" + IRRes.payload.toHexString())



        }

    }

    private fun sendEncryptCommand(payload: SSM2Payload, isCipher: DeviceSegmentType = DeviceSegmentType.cipher, onResponse: SesameOS2ResponseCallback) {
        CoroutineScope(IO).launch {
            semaphore.send(onResponse)
//            L.d("hcia", "🀄Command: ==>:" + payload.itemCode + " " + payload.opCode)
            gattTxBuffer = (SesameBleTransmit(isCipher, if (isCipher == DeviceSegmentType.cipher) cipher!!.encrypt(payload.toDataWithHeader()) else payload.toDataWithHeader()))
            transmit()
        }
    }

    private fun transmit() {
        mCharacteristic ?: return //todo check return work
        mCharacteristic?.value = gattTxBuffer?.getChunk() ?: return

//        L.d("hcia", "[app][say]:" + mCharacteristic?.value?.toHexString())
//        mBluetoothGatt?.writeCharacteristic(mCharacteristic)
        val check = mBluetoothGatt?.writeCharacteristic(mCharacteristic)
//        L.d("hcia", "[app][say]:" + mCharacteristic?.value?.toHexString() + " check:" + check)

        if (check == false) {
            disconnect { }
        }
    }

    private fun onGattSesamePublish(receivePayload: SSM3PublishPayload) {
        if (receivePayload.cmdItCode == SesameItemCode.login.value) {
            val loginResponse = SSM2LoginResponsePayload(receivePayload.payload)
            sendEncryptCommand(SSM2Payload(SSM2OpCode.update, SesameItemCode.timePhone, System.currentTimeMillis().toUInt32ByteArray())) {
                mResultRegister?.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
            mechStatus = loginResponse.SSM2MechStatus
            mechSetting = loginResponse.SSM2MechSetting
            deviceStatus = if (!mechSetting!!.isConfigured) CHDeviceStatus.NoSettings else if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
        }
        if (receivePayload.cmdItCode == SesameItemCode.initial.value) {
            mSesameToken = receivePayload.payload
            if (isRegistered) {
                if (isNeedAuthFromServer == true) {
                    val userIdx = sesame2KeyData!!.keyIndex.hexStringToByteArray()
                    val appPublicKeyBytes = EccKey.getPubK().hexStringToByteArray()
                    val sessionToken = mAppToken + mSesameToken
                    val signPayload = userIdx + appPublicKeyBytes + sessionToken
                    CHAccountManager.signGuestKey(CHRemoveSignKeyRequest(deviceId.toString().uppercase(), signPayload.toHexString(), sesame2KeyData!!.secretKey)) {
                        it.onSuccess {
                            login(it.data)
                        }
                        it.onFailure {}
                    }


                } else {
                    login()
                }
            } else {
                deviceStatus = CHDeviceStatus.ReadyToRegister
            }
        }


        if (receivePayload.cmdItCode == SesameItemCode.mechStatus.value) {
            mechStatus = CHSesame2MechStatus(receivePayload.payload)
            if ((mechStatus as CHSesame2MechStatus).retCode != 0) {
                readHistoryCommand {}
            } else if (mechStatus!!.target == Short.MIN_VALUE) {
                readHistoryCommand {}
            }
            deviceStatus = if (!mechSetting!!.isConfigured) CHDeviceStatus.NoSettings else if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else if ((mechStatus as CHSesame2MechStatus)!!.isInUnlockRange) CHDeviceStatus.Unlocked else CHDeviceStatus.Moved
        }
    }

    override fun configureLockPosition(lockTarget: Short, unlockTarget: Short, result: CHResult<CHEmpty>) {
        val payload = CHSesameLockPositionConfiguration((lockTarget.toInt() * 1024 / 360).toShort(), (unlockTarget.toInt() * 1024 / 360).toShort()).toPayload() + sesame2KeyData!!.createHistag(null)
        val cmd = SSM2Payload(SSM2OpCode.update, SesameItemCode.mechSetting, payload)
        sendEncryptCommand(cmd) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                mechSetting = CHSesame2MechSettings(CHSesameLockPositionConfiguration((lockTarget.toInt() * 1024 / 360).toShort(), (unlockTarget.toInt() * 1024 / 360).toShort()).toPayload())
                deviceStatus = if (!mechSetting!!.isConfigured) CHDeviceStatus.NoSettings else if (mechStatus!!.isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }

    override fun reset(result: CHResult<CHEmpty>) {
        sendEncryptCommand(SSM2Payload(SSM2OpCode.delete, SesameItemCode.registration, byteArrayOf())) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                dropKey(result)
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }

    override fun updateFirmware(result: CHResult<BluetoothDevice>) {
        L.d("hcia", "啟動dfu" + deviceStatus + " " + isRegistered)
        if (isRegistered) {
            if (checkBle(result)) return
            sendEncryptCommand(SSM2Payload(SSM2OpCode.update, SesameItemCode.enableDFU, "01".hexStringToByteArray())) { res ->
                if (res.cmdResultCode == SesameResultCode.success.value) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(advertisement?.device!!)))
                } else {
                    result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
                }
            }
        } else {
            sendEncryptCommand(SSM2Payload(SSM2OpCode.update, SesameItemCode.enableDFU, "01".hexStringToByteArray()), DeviceSegmentType.plain) { res ->
                if (res.cmdResultCode == SesameResultCode.success.value) {
                    result.invoke(Result.success(CHResultState.CHResultStateBLE(advertisement?.device!!)))
                } else {
                    result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
                }
            }
        }
    }

    override fun getHistories(cursor: Long?, result: CHResult<Pair<List<CHSesame2History>, Long?>>) {
        historyCallback = result
        CHAccountManager.getHistory(this, cursor) {
            it.onSuccess {
                val chHistorysToUI = ArrayList<CHSesame2History>()
                it.data.histories.forEach {
                    val historyType = Sesame2HistoryTypeEnum.getByValue(it.type)
                    val ts = it.timeStamp
                    val recordID = it.recordID
                    val histag = it.historyTag?.base64decodeByteArray()
//                    val params = it.parameter?.base64decodeByteArray()
                    val tmphis = eventToHistory(historyType, ts, recordID, histag)
                    if (tmphis != null) {
                        chHistorysToUI.add(tmphis)
                    }
                }
//                L.d("bbtig", "Response-Cursor: ${it.data.cursor}")

                result.invoke(Result.success(CHResultState.CHResultStateNetworks(Pair(chHistorysToUI.toList(), it.data.cursor))))
            }
            it.onFailure {
                result.invoke(Result.failure(it))
            }
        } //end getHistory
    }

    private fun eventToHistory(historyType: Sesame2HistoryTypeEnum?, ts: Long, recordID: Int, histag: ByteArray?): CHSesame2History? {
        when (historyType) { //            Sesame2HistoryTypeEnum.NONE -> return CHSesame2History.None(ts, recordID, histag)
            Sesame2HistoryTypeEnum.DRIVE_UNLOCKED -> return CHSesame2History.DriveUnLocked(ts, recordID, histag)
            Sesame2HistoryTypeEnum.DRIVE_LOCKED -> return CHSesame2History.DriveLocked(ts, recordID, histag)
            Sesame2HistoryTypeEnum.MANUAL_ELSE -> return CHSesame2History.ManualElse(ts, recordID, histag)
            Sesame2HistoryTypeEnum.MANUAL_UNLOCKED -> return CHSesame2History.ManualUnlocked(ts, recordID, histag)
            Sesame2HistoryTypeEnum.MANUAL_LOCKED -> return CHSesame2History.ManualLocked(ts, recordID, histag)
            Sesame2HistoryTypeEnum.WM2_LOCK -> return CHSesame2History.WM2Lock(ts, recordID, histag)
            Sesame2HistoryTypeEnum.WM2_UNLOCK -> return CHSesame2History.WM2Unlock(ts, recordID, histag)
            Sesame2HistoryTypeEnum.BLE_LOCK -> return CHSesame2History.BLELock(ts, recordID, histag)
            Sesame2HistoryTypeEnum.BLE_UNLOCK -> return CHSesame2History.BLEUnlock(ts, recordID, histag)
            Sesame2HistoryTypeEnum.WEB_LOCK -> return CHSesame2History.WEBLock(ts, recordID, histag)
            Sesame2HistoryTypeEnum.WEB_UNLOCK -> return CHSesame2History.WEBUnlock(ts, recordID, histag)
            Sesame2HistoryTypeEnum.AUTOLOCK -> return CHSesame2History.AutoLock(ts, recordID, histag)
            else -> return null
        }
    }

    private fun readHistoryCommand(result: CHResult<CHEmpty>) {
        if (checkBle(result)) return

        sendEncryptCommand(SSM2Payload(SSM2OpCode.read, SesameItemCode.history, if (isInternetAvailable()) byteArrayOf(0x01) else byteArrayOf(0x00))) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                if (isInternetAvailable()) {
//                    L.d("hcia", "deviceId.toString().uppercase():" + deviceId.toString().uppercase())

                }
                val recordId = res.payload.sliceArray(0..3).toBigLong().toInt()
                var historyType = Sesame2HistoryTypeEnum.getByValue(res.payload[4]) ?: Sesame2HistoryTypeEnum.NONE
                val newTime = res.payload.sliceArray(5..12).toBigLong() //4
//                L.d("hcia", "newTime:" + newTime)
                val historyContent = res.payload.sliceArray(13..res.payload.count() - 1)

                if (historyType == Sesame2HistoryTypeEnum.BLE_LOCK) {
                    val payload22 = historyContent.sliceArray(18..39)
                    val locktype = payload22[0] / 30
                    if (locktype == 1) {
                        historyType = Sesame2HistoryTypeEnum.WEB_LOCK
                    }
                    if (locktype == 2) {
                        historyType = Sesame2HistoryTypeEnum.WEB_LOCK
                    }
                    historyContent[18] = (payload22[0] % 30).toByte()

                }
                if (historyType == Sesame2HistoryTypeEnum.BLE_UNLOCK) {
                    val payload22 = historyContent.sliceArray(18..39)
                    val locktype = payload22[0] / 30
                    if (locktype == 1) {
                        historyType = Sesame2HistoryTypeEnum.WEB_UNLOCK
                    }
                    if (locktype == 2) {
                        historyType = Sesame2HistoryTypeEnum.WEB_UNLOCK
                    }
                    historyContent[18] = (payload22[0] % 30).toByte()
                }

                val chHistoryEvent: CHHistoryEvent = parseHistoryContent(historyType, historyContent, newTime, recordId)
                val historyEventToUpload: ArrayList<CHHistoryEvent> = ArrayList()

                historyEventToUpload.add(chHistoryEvent)
                val chHistorysToUI = ArrayList<CHSesame2History>()
                historyEventToUpload.forEach {
                    val ss2historyType = Sesame2HistoryTypeEnum.getByValue(it.type) ?: Sesame2HistoryTypeEnum.NONE
                    val ts = it.timeStamp
                    val recordID = it.recordID
                    val histag = it.historyTag?.base64decodeByteArray()
//                    val params = it.parameter?.base64decodeByteArray()
                    val tmphis = eventToHistory(ss2historyType, ts, recordID, histag)
                    if (tmphis != null) {
                        chHistorysToUI.add(tmphis)
//                        L.d("hcia", "tmphis:" + tmphis.date +" "+ tmphis.recordID)
                    }
                }

                historyCallback?.invoke(Result.success(CHResultState.CHResultStateBLE(Pair(chHistorysToUI.toList(), null))))
                if (isInternetAvailable()) {
                    this.readHistoryCommand {}
                }
            } else {
                historyCallback?.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }


}
internal fun parseHistoryContent(historyType: Sesame2HistoryTypeEnum, hisContent_: ByteArray, time: Long, recordId: Int): CHHistoryEvent {
//        L.d("hcia", "recordId:$recordId:---->historyType:" + historyType)
    val historyItem = CHHistoryEvent(recordId, null, historyType.value, time, null, null, null)
    when (historyType) {
        Sesame2HistoryTypeEnum.BLE_LOCK, Sesame2HistoryTypeEnum.BLE_UNLOCK, Sesame2HistoryTypeEnum.WM2_UNLOCK, Sesame2HistoryTypeEnum.WM2_LOCK -> {
            val key_idx = hisContent_.sliceArray(0..1).toBigLong()
            val device16 = hisContent_.sliceArray(2..17)
            val payload22 = hisContent_.sliceArray(18..39)
            historyItem.keyidx = key_idx
            historyItem.devicePk = device16.toHexString()
            historyItem.historyTag = payload22.toCutedHistag()?.base64Encode()
        }
        else -> {}
    }
    return historyItem
}
internal class SSM2LoginResponsePayload(loginPayload: ByteArray) {
    var systemTime = loginPayload.sliceArray(0..3).toBigLong()
    var fw_version = loginPayload[4]
    val historyCnt: Byte = loginPayload[6]
    val mech_setting_t = loginPayload.sliceArray(8..19)
    val mech_status_t = loginPayload.sliceArray(20..27)
    var SSM2MechStatus = CHSesame2MechStatus(mech_status_t)
    var SSM2MechSetting = CHSesame2MechSettings(mech_setting_t)
}
internal class CHSesameLockPositionConfiguration(private val lockTarget: Short, private val unlockTarget: Short) {
    private val range = 150
    private val lockRangeMin: Short = (lockTarget - range).toShort()
    private val lockRangeMax: Short = (lockTarget + range).toShort()
    private val unlockRangeMin: Short = (unlockTarget - range).toShort()
    private val unlockRangeMax: Short = (unlockTarget + range).toShort()

    internal fun toPayload(): ByteArray {
        return lockTarget.toReverseBytes() + unlockTarget.toReverseBytes() + lockRangeMin.toReverseBytes() + lockRangeMax.toReverseBytes() + unlockRangeMin.toReverseBytes() + unlockRangeMax.toReverseBytes()
    }
}