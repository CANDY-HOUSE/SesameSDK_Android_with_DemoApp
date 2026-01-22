package co.candyhouse.sesame.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.base64decodeHex
import co.candyhouse.sesame.utils.noHashtoUUID
import co.candyhouse.sesame.utils.toBigLong
import co.candyhouse.sesame.utils.toHexString
import java.util.UUID
import kotlin.experimental.and


internal interface CHBaseAdv {
    val rssi: Int?
    val isRegistered: Boolean
    val adv_tag_b1: Boolean
    val deviceID: UUID?
    var device: BluetoothDevice
    var deviceName: String?
    var productModel: CHProductModel?
    var isConnecable: Boolean?

}

internal class CHadv(scanResult: ScanResult) : CHBaseAdv {
//    var lastLiveTime = Date().time//System.currentTimeMillis()

    override var isConnecable: Boolean? = true
    private val advBytes = scanResult.scanRecord?.manufacturerSpecificData?.valueAt(0)!!

    override var adv_tag_b1: Boolean = (advBytes[2] and 2) > 0
    override val rssi: Int = scanResult.rssi
    override var device = scanResult.device
    override var deviceName = scanResult.scanRecord?.deviceName
    override var productModel: CHProductModel? = CHProductModel.getByValue(advBytes.copyOfRange(0, 1).toBigLong().toInt())

    override var isRegistered: Boolean =
            if (productModel == CHProductModel.Hub3) {
                (advBytes[1] and 1) > 0 // 1:registered 0:unregistered， Hub3 在Matter的二合一广播里，压缩去掉了机型的保留字。所以这里要用第二个字节advBytes[1]。
            } else {
                (advBytes[2] and 1) > 0
            }

    override val deviceID: UUID?
        get() {
            when (productModel) {
                CHProductModel.WM2 -> return try {
                    isConnecable = (advBytes.last()?.toInt() == 0)
                    val wm2ID = ("00000000055afd810001" + advBytes.copyOfRange(3, 9).toHexString()).noHashtoUUID()
                    wm2ID
                } catch (e: Exception) {
                    L.d("hcia", "e:" + e)
                    null
                }

                CHProductModel.Hub3 -> return try {
//                    L.d("hub3", "advBytes:" + advBytes.toHexString())
                    val hub3ID = ("00000000055afd810d00" + advBytes.copyOfRange(2, 8).toHexString()).noHashtoUUID()
//                    L.d("hub3", "uuid:" + hub3ID)
                    hub3ID
                } catch (e: Exception) {
                    L.d("hcia", "e:" + e)
                    null
                }

                CHProductModel.SS2, CHProductModel.SS4, CHProductModel.SesameBot1, CHProductModel.BiKeLock -> return try {
                    val tmpID = (deviceName + "==").base64decodeHex().noHashtoUUID()
                    tmpID
                } catch (e: Exception) {
//                    L.d("hcia", "e:" + e)
                    null
                }

                CHProductModel.SS5, CHProductModel.SS5PRO, CHProductModel.SSMOpenSensor, CHProductModel.SSMTouchPro, CHProductModel.SSMTouch2Pro, CHProductModel.SSMTouch, CHProductModel.SSMTouch2, CHProductModel.BiKeLock2, CHProductModel.BLEConnector, CHProductModel.Hub3, CHProductModel.Remote, CHProductModel.RemoteNano, CHProductModel.SS5US, CHProductModel.SesameBot2, CHProductModel.SSMFace, CHProductModel.SSMFace2, CHProductModel.SSMFacePro,
                CHProductModel.SSMFace2Pro, CHProductModel.SSMFaceAI,  CHProductModel.SSMFace2AI, CHProductModel.SSMFaceProAI, CHProductModel.SSMFace2ProAI, CHProductModel.SS6Pro, CHProductModel.SSMOpenSensor2, CHProductModel.SSM_MIWA -> return try {
//                    L.d("hcia", "[ss5] isRegistered:" + isRegistered)
//                    L.d("hcia", "[ss5] advBytes:" + advBytes.toHexString())
//                    L.d("hcia", "[ss5] deviceName:" + deviceName)
//                    L.d("hcia", "[ss5] deviceName hex:" + sss.scanRecord?.deviceName?.toByteArray()?.toHexString())
                    val uuidDATA = advBytes.sliceArray(3..18)
//                    L.d("hcia", "uuidDATA.toHexString():" + uuidDATA.toHexString())
                    val tmpID = uuidDATA.toHexString().noHashtoUUID()
                    tmpID
                } catch (e: Exception) {
//                    L.d("hcia", "e:" + e)
                    return UUID.fromString("1bfe50d8-0000-1111-1111-199ceff15268")
                }

                else -> {
                    return UUID.fromString("1bfe50d8-0000-4e8a-95b8-199ceff15268")
                }
            }
        }
}

