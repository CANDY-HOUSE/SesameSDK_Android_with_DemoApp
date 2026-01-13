package co.candyhouse.sesame.ble.os2

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.pm.PackageManager
import android.os.Build
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.CHadv
import co.candyhouse.sesame.ble.DeviceSegmentType
import co.candyhouse.sesame.ble.SSM2OpCode
import co.candyhouse.sesame.ble.SSM2ResponsePayload
import co.candyhouse.sesame.ble.SSM3PublishPayload
import co.candyhouse.sesame.ble.Sesame2Chracs
import co.candyhouse.sesame.ble.SesameBleTransmit
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.SesameNotifypayload
import co.candyhouse.sesame.ble.SesameResultCode
import co.candyhouse.sesame.ble.isBleAvailable
import co.candyhouse.sesame.ble.os2.base.CHSesameOS2
import co.candyhouse.sesame.ble.os2.base.SSM2Payload
import co.candyhouse.sesame.ble.os2.base.SesameOS2BleCipher
import co.candyhouse.sesame.ble.os2.base.SesameOS2ResponseCallback
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.db.model.createHistag
import co.candyhouse.sesame.db.model.hisTagC
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHBleManager.appContext
import co.candyhouse.sesame.open.CHBleManager.bluetoothAdapter
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.open.CHScanStatus
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.open.device.CHSesame2MechStatus
import co.candyhouse.sesame.open.device.CHSesameBot
import co.candyhouse.sesame.open.device.CHSesameBotMechSettings
import co.candyhouse.sesame.open.device.CHSesameBotMechStatus
import co.candyhouse.sesame.open.device.NSError
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.CHIotManager
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.server.dto.CHRemoveSignKeyRequest
import co.candyhouse.sesame.server.dto.CHSS2RegisterReq
import co.candyhouse.sesame.server.dto.CHSS2RegisterReqSig1
import co.candyhouse.sesame.utils.EccKey
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame.utils.base64Encode
import co.candyhouse.sesame.utils.base64decodeByteArray
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.isInternetAvailable
import co.candyhouse.sesame.utils.toHexString
import co.candyhouse.sesame.utils.toUInt32ByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.UUID

@SuppressLint("MissingPermission") internal class CHSesameBotDevice() : CHSesameOS2(), CHSesameBot, CHDeviceUtil {
    var isConnectedByWM2: Boolean = false

    override var mechSetting: CHSesameBotMechSettings? = null

    override var advertisement: CHadv? = null
        set(value) {
            field = value
            if (value == null) {
//                L.d("hcia", "[ssm] [adv] end:" + value)
                rssi = -100
                deviceStatus = CHDeviceStatus.NoBleSignal
                return
            }
//            by lazy { advertisement?.rssi }
            rssi = advertisement?.rssi
//            L.d("hcia", "rssi:" + rssi)
            deviceId = advertisement!!.deviceID
            isRegistered = advertisement!!.isRegistered

            if (deviceStatus.value == CHDeviceLoginStatus.logined && !isRegistered) {
                deviceStatus = CHDeviceStatus.Reset
            }

            if (deviceStatus == CHDeviceStatus.NoBleSignal) {
                deviceStatus = CHDeviceStatus.ReceivedAdV
            }

            if (deviceStatus == CHDeviceStatus.WaitingForAuth && isInternetAvailable()) {
                deviceStatus = CHDeviceStatus.ReceivedAdV
            }

        }


    override fun goIOT() {
//        L.d("hcia", "goIOT:")
        CHIotManager.subscribeSesame2Shadow(this) {
            it.onSuccess {
                if (deviceStatus.value == CHDeviceLoginStatus.unlogined) {
//                    L.d("hcia", "[bot 影子] it.data.state.reported.mechst:" + it.data.state.reported.mechst)
                    it.data.state.reported.mechst?.let {
                        var tmp = CHSesameBotMechStatus(it.hexStringToByteArray())
                        tmp.isStop = when (tmp.motorStatus) {//noPower: 0, forward: 1, hold:2, backward: 3
                            0.toByte() -> true
                            1.toByte() -> false
                            2.toByte() -> true
                            3.toByte() -> false
                            else -> false
                        }
                        mechStatus = tmp
//                        L.d("hcia", "[bot 影子] intention:" + intention)
                    }
                }
                it.data.state.reported.wm2s?.let { wm2s ->
                    isConnectedByWM2 = wm2s.map { it.value.hexStringToByteArray().first().toInt() }.contains(1)
                }

                if (isConnectedByWM2) {
                    deviceShadowStatus = if ((mechStatus as CHSesameBotMechStatus).isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
                } else {
                    deviceShadowStatus = null
                }

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


        if (CHBleManager.connectR.indexOf(advertisement?.device?.address) == -1) {
//            L.b("hcia", customDeviceName + ":連接紀錄:" + advertisement!!.device.address)
            CHBleManager.connectR.add(advertisement!!.device.address)
        } else {
//            L.b("hcia", customDeviceName + ":我已經連接過了：" + (advertisement as CHadv).device.address)
            return
        }

        deviceStatus = CHDeviceStatus.BleConnecting
        result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))

//        L.d("hcia", "Build.VERSION.SDK_INT:" + Build.VERSION.SDK_INT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                L.d("hcia", "ssm" + ":連接 O:")
            bluetoothAdapter.getRemoteDevice(advertisement!!.device.address).connectGatt(appContext, false, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                L.d("hcia", "ssm" + ":連接 M:")
            bluetoothAdapter.getRemoteDevice(advertisement!!.device.address).connectGatt(appContext, false, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
//                L.d("hcia", "ssm" + ":主動連接 old:")
            bluetoothAdapter.getRemoteDevice(advertisement!!.device.address).connectGatt(appContext, false, mBluetoothGattCallback)
        }
    }

    private val mBluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
            val ssmSay = gattRxBuffer.feed(characteristic!!.value)
            if (ssmSay?.first == DeviceSegmentType.cipher) {
                parceNotifyPayload(cipher!!.decrypt(ssmSay.second))
            }
            if (ssmSay?.first == DeviceSegmentType.plain) {
                parceNotifyPayload(ssmSay.second)
            }
        }

        private fun parceNotifyPayload(palntext: ByteArray) {
            val ssm2notify = SesameNotifypayload(palntext)//1
            if (ssm2notify.notifyOpCode == SSM2OpCode.publish) {
                val ssm2pubPayload = SSM3PublishPayload(ssm2notify.payload)
                onGattSesamePublish(ssm2pubPayload)
            }
            if (ssm2notify.notifyOpCode == SSM2OpCode.response) {
                val ssm2responsePayload = SSM2ResponsePayload(ssm2notify.payload)
                onGattSesameResponse(ssm2responsePayload)
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
                if (service.uuid == Sesame2Chracs.uuidService01) {
                    for (charc in service.characteristics) {
//                        L.d("hcia", "BluetoothGatt 特徵:" + charc.uuid)
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
            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                    L.d("hcia", "[bot] 連接狀態＋＋" + BleBaseType.GattConnectStateDec(newState) + " 狀態:" + BleBaseType.GattConnectStatusDec(status))
                deviceStatus = CHDeviceStatus.DiscoverServices
                mBluetoothGatt = gatt
                gatt.discoverServices()
            } else {
//                L.d("hcia", "[bot] 連接狀態 --" + BleBaseType.GattConnectStateDec(newState) + " 狀態:" + BleBaseType.GattConnectStatusDec(status))
                gatt.close()
                advertisement = null
                mBluetoothGatt = null
                CHBleManager.connectR.remove(gatt.device.address)
            }
        }
    }

    private fun onGattSesameResponse(ssm2ResponsePayload: SSM2ResponsePayload) {
//        val tx = gattTxBuffer
//        gattTxBuffer = null
//        L.d("hcia", "🀄Command:<==:" + ssm2ResponsePayload.cmdOPCode + "-" + ssm2ResponsePayload.cmdItCode + "-" + ssm2ResponsePayload.cmdResultCode)
        CoroutineScope(IO).launch {
//            L.d("hcia","解鎖<-------")
            semaphore.receive().invoke(ssm2ResponsePayload)
        }
//        tx?.onResponse?.invoke(ssm2ResponsePayload)
    }

    private fun onGattSesamePublish(receivePayload: SSM3PublishPayload) {

//        L.d("hcia", "receivePayload.cmdItCode:" + receivePayload.cmdItCode)
        if (receivePayload.cmdItCode == SesameItemCode.login.value) {

            L.d("hcia", "註冊完畢收到login:")

            val loginResponse = SSMBotLoginResponsePayload(receivePayload.payload)
            L.d("hcia", ":登入成功 loginResponse:" + loginResponse + " 歷史數量:" + loginResponse.historyCnt)
//            L.d("hcia", "loginResponse.fw_version:" + loginResponse.fw_version)//0
            sendEncryptCommand(SSM2Payload(SSM2OpCode.update, SesameItemCode.timePhone, System.currentTimeMillis().toUInt32ByteArray())) { }

            mechStatus = loginResponse.SSMBotMechStatus
            mechSetting = loginResponse.SSMBotMechSetting

            var tmp = loginResponse.SSMBotMechStatus
            tmp.isStop = when (tmp.motorStatus) {//noPower: 0, forward: 1, hold:2, backward: 3
                0.toByte() -> true
                1.toByte() -> false
                2.toByte() -> true
                3.toByte() -> false
                else -> false
            }
            mechStatus = tmp

//            L.d("hcia", "[bot] "+receivePayload.payload.toHexString() + " movingT:" + (mechStatus as CHSesameBotMechStatus).movingTimeInterval + " isLock:" + (mechStatus as CHSesameBotMechStatus).isInLockRange + " isUnlock:" + (mechStatus as CHSesameBotMechStatus).isInUnlockRange + " motor:" + (mechStatus as CHSesameBotMechStatus).motorStatus + " retCode:" + (mechStatus as CHSesameBotMechStatus).retCode)
//            intention = when ((mechStatus as CHSesameBotMechStatus).motorStatus) {//noPower: 0, forward: 1, hold:2, backward: 3
//                0.toByte() -> CHSesame2Intention.idle
//                1.toByte() -> CHSesame2Intention.locking
//                2.toByte() -> CHSesame2Intention.holding
//                3.toByte() -> CHSesame2Intention.unlocking
//                else -> CHSesame2Intention.movingToUnknownTarget
//            }
            deviceStatus = if ((mechStatus as CHSesameBotMechStatus).isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked

        }
        if (receivePayload.cmdItCode == SesameItemCode.initial.value) {
            mSesameToken = receivePayload.payload
//            L.d("hcia", "[bot] BluetoothGatt " + deviceId + " 收到mSesameToken:" + mSesameToken.toHexString())
            if (isRegistered) {
                if (isNeedAuthFromServer == true) {
                    val userIdx = sesame2KeyData!!.keyIndex.hexStringToByteArray()
                    val appPublicKeyBytes = EccKey.getPubK().hexStringToByteArray()
                    val sessionToken = mAppToken + mSesameToken
                    val signPayload = userIdx + appPublicKeyBytes + sessionToken
                    CHAPIClientBiz.signGuestKey(CHRemoveSignKeyRequest(deviceId.toString().toUpperCase(), signPayload.toHexString(), sesame2KeyData!!.secretKey)) {
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
//            L.d("hcia", "BOT mechStatus:" + receivePayload.payload.toHexString())

            var tmp = CHSesameBotMechStatus(receivePayload.payload)
            tmp.isStop = when (tmp.motorStatus) {//noPower: 0, forward: 1, hold:2, backward: 3
                0.toByte() -> true
                1.toByte() -> false
                2.toByte() -> true
                3.toByte() -> false
//                0.toByte() -> CHSesame2Intention.idle
//                1.toByte() -> CHSesame2Intention.locking
//                2.toByte() -> CHSesame2Intention.holding
//                3.toByte() -> CHSesame2Intention.unlocking
                else -> false
            }
            mechStatus = tmp
            deviceStatus = if ((mechStatus as CHSesameBotMechStatus).isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked

        }
    }


    override fun toggle(historyTag: ByteArray?, result: CHResult<CHEmpty>) {
        if (deviceStatus.value == CHDeviceLoginStatus.unlogined && isConnectedByWM2) {
            CHAPIClientBiz.cmdSesame(SesameItemCode.toggle, this, sesame2KeyData!!.hisTagC(historyTag), result)
            return
        }
        if ((mechStatus as CHSesame2MechStatus).isInLockRange) {
            unlock { it.onSuccess { result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty()))) } }
        } else {
            lock { it.onSuccess { result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty()))) } }
        }
    }

    override fun lock(historyTag: ByteArray?, result: CHResult<CHEmpty>) {

        if (deviceStatus.value == CHDeviceLoginStatus.unlogined && isConnectedByWM2) {
            CHAPIClientBiz.cmdSesame(SesameItemCode.lock, this, sesame2KeyData!!.hisTagC(historyTag), result)
            return
        }
        val his = sesame2KeyData!!.createHistag(historyTag)


        sendEncryptCommand(SSM2Payload(SSM2OpCode.async, SesameItemCode.lock, his)) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }

    override fun unlock(historyTag: ByteArray?, result: CHResult<CHEmpty>) {
        if (deviceStatus.value == CHDeviceLoginStatus.unlogined && isConnectedByWM2) {
            CHAPIClientBiz.cmdSesame(SesameItemCode.unlock, this, sesame2KeyData!!.hisTagC(historyTag), result)
            return
        }
        val his = sesame2KeyData!!.createHistag(historyTag)
        sendEncryptCommand(SSM2Payload(SSM2OpCode.async, SesameItemCode.unlock, his)) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }

    override fun click(historyTag: ByteArray?, result: CHResult<CHEmpty>) {

//        L.d("hcia", "click deviceStatus.value:" + deviceStatus.value)
//        L.d("hcia", "isConnectedByWM2:" + isConnectedByWM2)
        if (deviceStatus.value == CHDeviceLoginStatus.unlogined && isConnectedByWM2) {
            CHAPIClientBiz.cmdSesame(SesameItemCode.click, this, sesame2KeyData!!.hisTagC(historyTag), result)

        }
        if (!isBleAvailable(result)) return

//        L.d("hcia", "🎃 historyTag:" + " " + sesame2KeyData!!.historyTag?.let { String(it) } + " " + sesame2KeyData!!.historyTag)
        val his = sesame2KeyData!!.createHistag(historyTag)
        sendEncryptCommand(SSM2Payload(SSM2OpCode.async, SesameItemCode.click, his)) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }

    override fun updateSetting(setting: CHSesameBotMechSettings, historyTag: ByteArray?, result: CHResult<CHEmpty>) {
        if (!isBleAvailable(result)) return

        val his = sesame2KeyData!!.createHistag(historyTag)
        sendEncryptCommand(SSM2Payload(SSM2OpCode.update, SesameItemCode.mechSetting, setting.data() + his)) { res ->
            if (res.cmdResultCode == SesameResultCode.success.value) {
                mechSetting = setting
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            } else {
                result.invoke(Result.failure(NSError(res.cmdResultCode.toString(), "CBCentralManager", res.cmdResultCode.toInt())))
            }
        }
    }

    override fun login(token: String?) {
//        L.d("hcia", "loginSesameBOT BluetoothGatt " + " mSSTK:" + mSesameToken.toHexString())
        deviceStatus = CHDeviceStatus.BleLogining
        val secret = sesame2KeyData!!.secretKey.hexStringToByteArray()
        val userIdx = sesame2KeyData!!.keyIndex.hexStringToByteArray()
        val ssmPublicKeyBytes = sesame2KeyData!!.sesame2PublicKey.hexStringToByteArray()
        val devicePublicKeyBytes = EccKey.getPubK().hexStringToByteArray()
        val sessionToken = mAppToken + mSesameToken

        val signPayload = userIdx + devicePublicKeyBytes + sessionToken
        val sessionAuth: ByteArray?
        if (isNeedAuthFromServer == true) {
            sessionAuth = token?.hexStringToByteArray()
        } else {
            sessionAuth = AesCmac(secret, 16).computeMac(signPayload)
        }
        val ecdhSecret = EccKey.ecdh(ssmPublicKeyBytes)
        val ecdhSecretPre16 = ecdhSecret.sliceArray(0..15)
        val sessionKey = AesCmac(ecdhSecretPre16, 16).computeMac(sessionToken)
        val loginPayload = userIdx + devicePublicKeyBytes + mAppToken + sessionAuth!!.sliceArray(0..3)
        semaphore = Channel(capacity = 1)
        cipher = SesameOS2BleCipher(sessionKey!!, sessionToken)
        val cmd = SSM2Payload(SSM2OpCode.sync, SesameItemCode.login, loginPayload)
        sendEncryptCommand(cmd, DeviceSegmentType.plain) { ssm2ResponsePayload ->
//            L.d("hcia", "[bot] 下指令登入成功")
            if (ssm2ResponsePayload.cmdItCode == SesameItemCode.login.value && ssm2ResponsePayload.cmdResultCode == SesameResultCode.success.value) {

//                L.d("hcia", "按鈕登入 payload:" + ssm2ResponsePayload.payload.toHexString())
                val loginResponse = SSMBotLoginResponsePayload(ssm2ResponsePayload.payload)
                val currentTimestamp = System.currentTimeMillis() / 1000
                val timeError = currentTimestamp.minus(loginResponse.systemTime)
//                L.d("hcia", "timeError:" + timeError)
                if (timeError > 3) { // check check  time error
//                    L.d("hcia", "登入後設定時間開始 ==>")
                    sendEncryptCommand(SSM2Payload(SSM2OpCode.update, SesameItemCode.timePhone, System.currentTimeMillis().toUInt32ByteArray())) { }
                }
                mechStatus = loginResponse.SSMBotMechStatus
                mechSetting = loginResponse.SSMBotMechSetting

                var tmp = loginResponse.SSMBotMechStatus
                tmp.isStop = when (tmp.motorStatus) {//noPower: 0, forward: 1, hold:2, backward: 3
                    0.toByte() -> true
                    1.toByte() -> false
                    2.toByte() -> true
                    3.toByte() -> false
                    else -> false
                }
                mechStatus = tmp

                deviceStatus = if ((mechStatus as CHSesameBotMechStatus).isInLockRange) CHDeviceStatus.Locked else CHDeviceStatus.Unlocked
            }
        }
    }

    override fun getVersionTag(result: CHResult<String>) {
        if (!isBleAvailable(result)) return

        sendEncryptCommand(SSM2Payload(SSM2OpCode.read, SesameItemCode.versionTag, byteArrayOf())) { res ->
            val gitTag = res.payload.sliceArray(4..15)
//            CHIotManager.updateSS2ShadowVertion(this, String(gitTag))
            result.invoke(Result.success(CHResultState.CHResultStateBLE(String(gitTag))))
        }
    }

    override fun register(resultRegister: CHResult<CHEmpty>) {
        if (deviceStatus != CHDeviceStatus.ReadyToRegister) {
            resultRegister.invoke(Result.failure(CHError.BleInvalidAction.value))
            return
        }
        semaphore = Channel(capacity = 1)
        sendEncryptCommand(SSM2Payload(SSM2OpCode.read, SesameItemCode.IRER, byteArrayOf()), DeviceSegmentType.plain) { IRRes ->
            val ER = IRRes.payload.drop(16).toByteArray().toHexString()

            L.d("hcia", "[bot] ER:" + ER)
            L.d("hcia", "註冊請求開始 ==> deviceStatus:" + deviceStatus + " deviceId:" + deviceId)
            deviceStatus = CHDeviceStatus.Registering
            val req = CHSS2RegisterReq(
                CHSS2RegisterReqSig1(
                    EccKey.getRegisterAK(),
                    mSesameToken.base64Encode(),
                    ER,
                    advertisement!!.productModel!!.productType().toString()
                )
            )
            CHAPIClientBiz.myDevicesRegisterSesame2Post(
                deviceId.toString(),
                req
            ) { apiResult ->
                apiResult.fold(
                    onSuccess = { state ->
                        val registerSesame1 = state.data
                        val sig1 = registerSesame1.sig1.base64decodeByteArray().sliceArray(0..3)
                        val appPubKey = EccKey.getPubK().hexStringToByteArray()
                        val serverToken = registerSesame1.st.base64decodeByteArray()
                        val sesamePublicKey = registerSesame1.pubkey.base64decodeByteArray()
                        val ecdhSecret = EccKey.ecdh(sesamePublicKey)
                        val ecdhSecretPre16 = ecdhSecret.sliceArray(0..15)
                        val payload = sig1 + appPubKey + serverToken
                        val cmd = SSM2Payload(SSM2OpCode.create, SesameItemCode.registration, payload)
                        L.d("hcia", "註冊指令==>")

                        val sessionToken = serverToken + mSesameToken
                        val registerKey = AesCmac(ecdhSecretPre16, 16).computeMac(sessionToken)
                        val ownerKey = AesCmac(registerKey!!, 16).computeMac("owner_key".toByteArray())
                        val sessionKey = AesCmac(registerKey, 16).computeMac(sessionToken)

                        cipher = SesameOS2BleCipher(sessionKey!!, sessionToken)
                        sendEncryptCommand(cmd, DeviceSegmentType.plain) { result ->
                            val candyDevice = CHDevice(
                                deviceId.toString(),
                                CHProductModel.SesameBot1.deviceModel(),
                                null,
                                "0000",
                                ownerKey!!.toHexString(),
                                sesamePublicKey.toHexString()
                            )
                            sesame2KeyData = candyDevice
                            CHDB.CHSS2Model.insert(candyDevice) {
                                resultRegister.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
                            }
                            L.d("hcia", "<===我成功註冊按鈕手了 device_id::" + deviceId)
                        }
                    },
                    onFailure = { err ->
                        resultRegister.invoke(Result.failure(err))
                    }
                )
            }
        }
    }


    override fun reset(result: CHResult<CHEmpty>) {
        L.d("hcia", "重置Sesame")
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
            if (!isBleAvailable(result)) return
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

    private fun sendEncryptCommand(payload: SSM2Payload, isCipher: DeviceSegmentType = DeviceSegmentType.cipher, onResponse: SesameOS2ResponseCallback) {
        CoroutineScope(IO).launch {
            semaphore.send(onResponse)
            sendCommand(SesameBleTransmit(isCipher, if (isCipher == DeviceSegmentType.cipher) cipher!!.encrypt(payload.toDataWithHeader()) else payload.toDataWithHeader()))
        }
    }

    private fun sendCommand(txClosure: SesameBleTransmit) {
//        CoroutineScope(IO).launch {
//            L.d("hcia","上鎖----->！"+payload.itemCode)
//            L.d("hcia", "🀄Command: ==>:" + payload.itemCode + " " + payload.opCode + " " + delegate)
        gattTxBuffer = txClosure
        transmit()
//        }
    }

    private fun transmit() {
        mCharacteristic ?: return
        val data = gattTxBuffer?.getChunk() ?: return
        mCharacteristic?.value = data
        mBluetoothGatt?.writeCharacteristic(mCharacteristic)
    }

}