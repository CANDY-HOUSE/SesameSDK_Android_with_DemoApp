package co.candyhouse.sesame.open.device.sesameBiometric.capability.connect

import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.ble.os3.base.CHSesameOS3
import co.candyhouse.sesame.ble.os3.base.SesameOS3Payload
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale.CHCapabilitySupport
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.base64Encode
import co.candyhouse.sesame.utils.hexStringToByteArray

internal class CHDeviceConnectCapableImpl() : CHDeviceConnectCapable {
    private var support: CHCapabilitySupport? = null
    fun setSupport(support: CHCapabilitySupport) {
        this.support = support
    }

    override fun insertSesame(sesame: CHDevices, result: CHResult<CHEmpty>) {
        if (support?.isBleAvailable(result) == false) return
        L.d("hcia", "送出鑰匙sesame.getKey():" + sesame.getKey())
        if (sesame is CHSesameOS3) {
            // SS5/5pro, bike2
            val ssm = sesame as CHDeviceUtil
            val noDashUUID = ssm.sesame2KeyData!!.deviceUUID.replace("-", "")
            val noDashUUIDDATA = noDashUUID.hexStringToByteArray()
            val ssmSecKa = ssm.sesame2KeyData!!.secretKey.hexStringToByteArray()

            support?.sendCommand(SesameOS3Payload(SesameItemCode.ADD_SESAME.value, noDashUUIDDATA + ssmSecKa)) { ssm2ResponsePayload ->
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
        } else {
            // SS3/4, bot1, bike1
            val ssm = sesame as CHDeviceUtil
            val noDashUUID = ssm.sesame2KeyData!!.deviceUUID.replace("-", "")
            val b64k = noDashUUID.hexStringToByteArray().base64Encode().replace("=", "")
            val ssmIRData = b64k.toByteArray()
            val ssmPKData = ssm.sesame2KeyData!!.sesame2PublicKey.hexStringToByteArray()
            val ssmSecKa = ssm.sesame2KeyData!!.secretKey.hexStringToByteArray()
            val allKey = ssmIRData + ssmPKData + ssmSecKa

            support?.sendCommand(SesameOS3Payload(SesameItemCode.ADD_SESAME.value, allKey)) {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
        }
    }

    override fun removeSesame(tag: String, result: CHResult<CHEmpty>) {
        if (support?.isBleAvailable(result) == false) return

        // 假设 support 实现了 CHSesameConnector 接口
        val ssm2KeysMap = (support as? CHSesameConnector)?.ssm2KeysMap ?: run {
            result.invoke(Result.failure(Exception("Device is not a connector")))
            return
        }

        val keysList = ssm2KeysMap[tag]
        if (keysList == null || keysList.isEmpty()) {
            result.invoke(Result.failure(Exception("Keys list is null or empty")))
            return
        }

        val firstKey = keysList[0] ?: run {
            result.invoke(Result.failure(Exception("First key is null")))
            return
        }

        if (firstKey.toInt() == 0x04) { // ss4
            val noDashUUID = tag.replace("-", "")
            val b64k = noDashUUID.hexStringToByteArray().base64Encode().replace("=", "")
            val ssmIRData = b64k.toByteArray()

            support?.sendCommand(SesameOS3Payload(SesameItemCode.REMOVE_SESAME.value, ssmIRData)) { ssm2ResponsePayload ->
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
        } else { // ss5
            val noDashUUID = tag.replace("-", "")

            support?.sendCommand(SesameOS3Payload(SesameItemCode.REMOVE_SESAME.value, noDashUUID.hexStringToByteArray())) { ssm2ResponsePayload ->
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
        }
    }

    override fun setRadarSensitivity(payload: ByteArray, result: CHResult<CHEmpty>) {
        L.d("sf", "蓝牙发送雷达灵敏度值：" + payload[1])

        support?.sendCommand(SesameOS3Payload(SesameItemCode.SSM_OS3_RADAR_PARAM_SET.value, payload)) { _ ->
            result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
        }
    }
}