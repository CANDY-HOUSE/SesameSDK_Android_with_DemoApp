package co.candyhouse.server

import android.content.Context
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.CHHub3IRCode
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IROperation
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.IRType
import co.candyhouse.server.dto.IrCodeChangeRequest
import co.candyhouse.server.dto.IrCodeDeleteRequest
import co.candyhouse.server.dto.IrDeviceAddRequest
import co.candyhouse.server.dto.IrDeviceDeleteRequest
import co.candyhouse.server.dto.IrDeviceModifyRequest
import co.candyhouse.server.dto.IrDeviceRemoteAddToMatterRequest
import co.candyhouse.server.dto.IrDeviceRemoteKeyRequest
import co.candyhouse.server.dto.IrDeviceStateRequest
import co.candyhouse.server.dto.IrLearnedDataAddRequest
import co.candyhouse.server.dto.IrMatchCodeRequest
import co.candyhouse.sesame.open.CHConfiguration
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.getClientRegion
import co.candyhouse.sesame.utils.toHexString
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.apigateway.ApiClientException
import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext



object CHIRAPIManager {
    private lateinit var jpAPIClient: CHIRAPIClient
    private var httpScope = CoroutineScope(IO)
    private var isInitialized = false

    fun initialize(context: Context) {
        setupAPI(context)
    }

    @Synchronized
    private fun setupAPI(appContext: Context) {
        if (isInitialized) {
            return
        }
        httpScope.launch(IO) {
            val credentialsProvider = CognitoCachingCredentialsProvider(
                appContext, CHConfiguration.CLIENT_ID,  // 身份集區 ID
                CHConfiguration.CLIENT_ID.getClientRegion() // 區域
            )
            val factory = ApiClientFactory().credentialsProvider(credentialsProvider).apiKey(
                CHConfiguration.API_KEY
            ).region("ap-northeast-1")
            jpAPIClient = factory.build(CHIRAPIClient::class.java)
            isInitialized = true
        }
    }


    private fun <T, R, A> T.makeApiCall(onResponse: CHResult<A>, block: T.() -> R) {
        httpScope.launch(EmptyCoroutineContext, CoroutineStart.DEFAULT) {
            runCatching {
                try {
                    block()
                } catch (e: Exception) {
                    e.printStackTrace()
                    onResponse.invoke(Result.failure(e))
                }

            }.onFailure {
                onResponse.invoke(Result.failure(it))
            }.onSuccess {}
        }
    }

    /**
     * 获取指定IR设备的按键列表
     * @param hubDeviceUuid 设备ID
     * @param irDeviceUuid IR设备ID
     * @param onResponse 回调
     */
    fun getIRCodes(
        hubDeviceUuid: String,
        irDeviceUuid: String,
        onResponse: CHResult<List<CHHub3IRCode>>
    ) {
        makeApiCall(onResponse) {
            try {
                val res = jpAPIClient.getIRCodes(hubDeviceUuid, irDeviceUuid)
                val jsonString = Gson().toJson(res)
                val type = object : TypeToken<List<CHHub3IRCode>>() {}.type
                val irKeyResponses = Gson().fromJson<List<CHHub3IRCode>>(jsonString, type)
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(irKeyResponses)))
            } catch (e: Exception) {
                L.e("CHIRAPIManager", "Error parsing IR keys", e)
                onResponse.invoke(Result.failure(e))
            }
        }
    }

    /**
     * 修改指定IR设备的按键名称
     * @param baseIrCode IR设备 key
     * @param name 新名称
     * @param onResponse 回调
     */
    fun changeIRCode(baseIrCode: CHHub3IRCode, name: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            try {
                val requestBody = IrCodeChangeRequest(baseIrCode.uuid, baseIrCode.irID, name)
                L.d("CHIRAPIManager", "changeIRCode:baseIrCode.deviceId=${baseIrCode.deviceId} body:${requestBody.toString()}")
                val res = jpAPIClient.changeIRCode(baseIrCode.deviceId, requestBody)
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            } catch (e: Exception) {
                L.e("CHIRAPIManager", "Error parsing IR keys", e)
                onResponse.invoke(Result.failure(e))
            }
        }
    }

    /**
     * 删除指定IR设备的按键
     * @param baseIrCode IR设备 key
     * @param onResponse 回调
     */
    fun deleteIRCode(baseIrCode: CHHub3IRCode, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            try {
                val requestBody = IrCodeDeleteRequest(baseIrCode.uuid, baseIrCode.irID)
                val res = jpAPIClient.deleteIRCode(baseIrCode.deviceId, requestBody)
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            } catch (e: Exception) {
                L.e("CHIRAPIManager", "Error parsing IR keys", e)
                onResponse.invoke(Result.failure(e))
            }
        }
    }

    /**
     * 修改IR设备信息
     * @param deviceId 设备ID
     * @param alias 设备别名
     */
    fun updateIRDevice(
        deviceId: String,
        uuid: String,
        alias: String = "",
        onResponse: CHResult<Any>
    ) {
        makeApiCall(onResponse) {
            try {
                val requestBody = IrDeviceModifyRequest(uuid, alias)
                val res = jpAPIClient.updateIRDevice(deviceId, requestBody)
                L.d("CHIRAPIManager", "updateIRDevice deviceId=${deviceId} body=${requestBody.toString()}   res: $res")
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            } catch (e: Exception) {
                L.e("CHIRAPIManager", "Error parsing IR keys", e)
                onResponse.invoke(Result.failure(e))
            }
        }
    }

    /**
     * 更新IR设备信息
     * @param deviceId 设备ID
     * @param alias 设备别名
     * @param state 设备状态
     */
    fun updateIRDeviceState(
        deviceId: String,
        uuid: String,
        state: String = "",
        onResponse: CHResult<Any>
    ) {
        makeApiCall(onResponse) {
            try {
                val requestBody = IrDeviceStateRequest(uuid, state)
                val res = jpAPIClient.updateIRDeviceState(deviceId, requestBody)
                L.d("CHIRAPIManager", "updateIRDeviceState:deviceId=${deviceId} requestBody=${requestBody.toString()} $res")
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            } catch (e: Exception) {
                L.e("CHIRAPIManager", "Error parsing IR keys", e)
                onResponse.invoke(Result.failure(e))
            }
        }
    }

    /**
     * 发送IR遥控器按键
     * @param deviceId 设备ID
     * @param command 命令内容
    * @param operation 操作类型, 默认为 "emit"{ "remoteEmit" 表示遥控器发送命令, "learnEmit" 表示学习命令，"modelSet" 表示设置模型, "modelGet" 表示获取模型 }
     * @param onResponse 回调,返回结果
     */
    fun emitIRRemoteDeviceKey(
        deviceId: String,
        command: String = "",
        operation: String = "",
        irDeviceUUID: String = "",
        irType: Int = 0,
        onResponse: CHResult<Any>
    ) {
        makeApiCall(onResponse) {
            try {
                val requestBody = IrDeviceRemoteKeyRequest(
                    data = command,
                    operation = operation,
                    irDeviceUUID = irDeviceUUID,
                    irType = irType
                )
                L.d("CHIRAPIManager", "emitIRRemoteDeviceKey:deviceId=${deviceId} requestBody=${requestBody}")
                val res = jpAPIClient.emitIRRemoteDeviceKey(deviceId, requestBody)
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            } catch (e: ApiClientException) {
                onResponse.invoke(Result.failure(e))
            } catch (e: Exception) {
                L.e("CHIRAPIManager", "Error parsing IR keys", e)
                onResponse.invoke(Result.failure(e))
            }
        }
    }

    /**
     *获取IR设备列表
     */
    fun fetchIRDevices(deviceUUID: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            try {
                val res = jpAPIClient.fetchIRDevices(deviceUUID)
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            } catch (e: Exception) {
                L.e("sf", "fetchIRDevices Error ", e)
                onResponse.invoke(Result.failure(e))
            }
        }
    }

    /**
     *删除IR遥控器
     */
    fun deleteIRDevice(deviceUUID: String, subID: String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            try {
                val requestBody = IrDeviceDeleteRequest(subID)
                val res = jpAPIClient.deleteIRDevice(deviceUUID, requestBody)
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            } catch (e: Exception) {
                L.e("sf", "deleteIRDevice Error ", e)
                onResponse.invoke(Result.failure(e))
            }
        }
    }

    /**
     *添加IR遥控器
     */
    fun addIRDevice(
        hub3: CHHub3,
        remoteDevice: IrRemote,
        state: String,
        irDeviceType: Int,
        irCodes: List<CHHub3IRCode>,
        onResponse: CHResult<Any>
    ) {
        makeApiCall(onResponse) {
            try {
                val hub3DdeviceId = hub3.deviceId.toString().uppercase()
                val model: String = if (null != remoteDevice.model) {
                    remoteDevice.model!!
                } else {
                    ""
                }
                var keys: Array<Map<String, String>> = emptyArray()
                if (irCodes.isNotEmpty()) {
                    keys = irCodes.map {
                        mapOf(
                            "keyUUID" to it.irID,
                            "name" to it.name
                        )
                    }.toTypedArray()
                }
                val requestBody = IrDeviceAddRequest(
                    uuid = remoteDevice.uuid,
                    model = model,
                    state = state,
                    alias = remoteDevice.alias,
                    type = irDeviceType,
                    deviceId = hub3DdeviceId,
                    code = remoteDevice.code,
                    keys = keys
                )
                L.d("addIRDevice", "addIRDevice: ${requestBody.toString()}")
                val res = jpAPIClient.addIRDeviceInfo(hub3DdeviceId, requestBody)
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            } catch (e: Exception) {
                L.e("sf", "addIRDevice Error ", e)
                e.printStackTrace()
                onResponse.invoke(Result.failure(e))
            }
        }
    }

    private fun formatToUUID(hex: String): String {
        if (hex.length != 32) {
            throw IllegalArgumentException("Invalid UUID hex string length: ${hex.length}")
        }
        return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}"
    }

    // 定义常量以提高可读性和可维护性
    private const val DATA_SIZE_MIN_NUM = 20

    // 输入的 data， 是 Hub3 通过 IoT 的 MQTT 主题上传的数据
    fun addHub3LearnedIrData(data: ByteArray, hub3DeviceUUID: String, irDataNameUUID: String, irDeviceUUID: String, keyUUID: String) {
        // 检查数据长度是否合法,
        //  ESP32C3的红外硬件数据格式， 一个位使用一对高低电平的持续时间长短来表示， 以2us为单位， 需要4个字节，
        //  红外遥控器的遥控信号发射的有效信息， 至少有5个bit， 也就是5对高低电平， 5 * 4 = 20
        if (data.size < DATA_SIZE_MIN_NUM) {
            L.e("addHub3LearnedIrData", "Invalid data length: ${data.size}")
            return
        }

        try {
            // 构建请求体
            val requestBody = IrLearnedDataAddRequest(
                irDataNameUUID = irDataNameUUID,
                dataLength = data.size,
                esp32c3LearnedIrDataHexString = data.toHexString(),
                timeStamp = System.currentTimeMillis(),
                hub3DeviceUUID = hub3DeviceUUID,
                irDeviceUUID = irDeviceUUID,
                keyUUID = keyUUID,
            )
            L.d("addHub3LearnedIrData", "requestBody: $requestBody")
            // 发送请求
            jpAPIClient.addHub3LearnedIrData(requestBody)
        } catch (e: Exception) {
            L.e("addHub3LearnedIrData", "Error processing data: ${e.message}", e)
        }
    }


    fun matchIrCode(data: ByteArray, type:Int, branchName:String, onResponse: CHResult<Any>): Boolean {

        if (data.size < DATA_SIZE_MIN_NUM) {
            L.e("matchIrCode", "Invalid data length: ${data.size}")
            return false
        }
        makeApiCall(onResponse) {
            try {
                val requestBody = IrMatchCodeRequest(
                    irWave = data.toHexString(),
                    irWaveLength = data.size,
                    type = type,
                    brandName = branchName
                )
                val res = jpAPIClient.matchIrCode(requestBody)
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            } catch (e: Exception) {
                L.e("sf", "matchIrCode Error ", e)
                onResponse.invoke(Result.failure(e))
            }
        }
        return true
    }

    /**
     * 设定IR模式:
     * @param model 0x01 进入学习模式, 0x00 进入普通控制模式
     * @param onResponse 回调
     */
    fun setIRMode(model: Int, hub3DeviceId:String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            try {
                val requestBody = IrDeviceRemoteKeyRequest(IROperation.OPERATION_MODE_SET, model.toString())
                val res = jpAPIClient.sendIRMode(hub3DeviceId, requestBody)
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            } catch (e: Exception) {
                L.e("CHIRAPIManager", "Error parsing IR keys", e)
                onResponse.invoke(Result.failure(e))
            }
        }
    }

    /**
     * 获取IR模式:
     * @param onResponse 回调
     */
    fun getIRMode(hub3DeviceId:String, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) {
            try {
                val requestBody = IrDeviceRemoteKeyRequest(IROperation.OPERATION_MODE_GET, "")
                val res = jpAPIClient.sendIRMode(hub3DeviceId, requestBody)
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            } catch (e: Exception) {
                L.e("CHIRAPIManager", "Error parsing IR keys", e)
                onResponse.invoke(Result.failure(e))
            }
        }
    }

    fun addIRRemoteDeviceToMatter(onCommand: String, offCommand: String, irRemote: IrRemote, hub3: CHHub3, onResponse: CHResult<Any>) {
        makeApiCall(onResponse){
            try {
                val requestBody = IrDeviceRemoteAddToMatterRequest(irDeviceType = IRType.DEVICE_REMOTE_LIGHT, onCommand, offCommand, irRemote.uuid.uppercase())
                L.d("harry", "addIRRemoteDeviceToMatter: ${requestBody.toString()}")
                val res = jpAPIClient.addIRRemoteDeviceToMatter(hub3.deviceId.toString().uppercase(), requestBody)
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res)))
            } catch (e: Exception) {
                L.e("CHIRAPIManager", "Error parsing IR keys", e)
                onResponse.invoke(Result.failure(e))
            }
        }
    }

    /**
     *获取遥控器列表
     */
    fun fetchRemoteList(brandType: Int, onResponse: CHResult<List<IrRemote>>) {
        makeApiCall(onResponse) {
            try {
                val res = jpAPIClient.fetchRemoteList(brandType)
                onResponse.invoke(Result.success(CHResultState.CHResultStateNetworks(res.data)))
            } catch (e: Exception) {
                L.e("CHIRAPIManager", "fetchIRDevices Error ", e)
                onResponse.invoke(Result.failure(e))
            }
        }
    }

}

