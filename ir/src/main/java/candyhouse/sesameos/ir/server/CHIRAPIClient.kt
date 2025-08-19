package candyhouse.sesameos.ir.server

import candyhouse.sesameos.ir.base.irHttpClientBean.IrCodeChangeRequest
import candyhouse.sesameos.ir.base.irHttpClientBean.IrCodeDeleteRequest
import candyhouse.sesameos.ir.base.irHttpClientBean.IrDeviceAddRequest
import candyhouse.sesameos.ir.base.irHttpClientBean.IrDeviceDeleteRequest
import candyhouse.sesameos.ir.base.irHttpClientBean.IrDeviceModifyRequest
import candyhouse.sesameos.ir.base.irHttpClientBean.IrDeviceRemoteKeyRequest
import candyhouse.sesameos.ir.base.irHttpClientBean.IrDeviceStateRequest
import candyhouse.sesameos.ir.base.irHttpClientBean.IrLearnedDataAddRequest
import candyhouse.sesameos.ir.base.irHttpClientBean.IrMatchCodeRequest
import co.candyhouse.sesame2.BuildConfig
import com.amazonaws.mobileconnectors.apigateway.annotation.Operation
import com.amazonaws.mobileconnectors.apigateway.annotation.Parameter
import com.amazonaws.mobileconnectors.apigateway.annotation.Service

@Service(endpoint = BuildConfig.chjpserver)
internal interface CHIRAPIClient {

    /**
     * 获取指定IR设备的按键列表
     *
     * @param deviceId 设备唯一标识符
     * @return 设备按键列表响应
     */
    @Operation(path = "/device/v2/ir/{device_id}/keys", method = "GET")
    fun getIRCodes(
        @Parameter(name = "device_id", location = "path") deviceId: String,
        @Parameter(name = "uuid", location = "query") uuid: String
    ): Any

    /**
     * 修改指定IR设备的key名称
     *
     * @param deviceId 设备唯一标识符
     * @param body 请求体: keyUUID, name 请求信息封装
     * @return 设备按键列表响应
     */
    @Operation(path = "/device/v2/ir/{device_id}/keys", method = "PUT")
    fun changeIRCode(
        @Parameter(name = "device_id", location = "path") deviceId: String,
        body: IrCodeChangeRequest
    ): Any

    /**
     * 删除指定IR设备的key
     *
     * @param deviceId 设备唯一标识符
     * @param body 请求体: keyUUID, uuid 请求信息封装
     * @return 设备按键列表响应
     */
    @Operation(path = "/device/v2/ir/{device_id}/keys", method = "DELETE")
    fun deleteIRCode(
        @Parameter(name = "device_id", location = "path") deviceId: String,
        body: IrCodeDeleteRequest
    ): Any

    /**
     * 添加IR设备信息
     *
     * @param deviceId 设备唯一标识符
     * @param body 请求体: uuid 请求信息封装 state 请求信息封装 alias 请求信息封装
     * @return 设备按键列表响应
     */
    @Operation(path = "/device/v2/ir/{device_id}", method = "POST")
    fun addIRDeviceInfo(
        @Parameter(name = "device_id", location = "path") deviceId: String,
        body: IrDeviceAddRequest
    ): Any


    @Operation(path = "/device/v2/ir/hub3_learned_ir_data", method = "POST")
    fun addHub3LearnedIrData(
        body: IrLearnedDataAddRequest
    ): Any

    /**
     * 更新IR设备信息
     *
     * @param deviceId 设备唯一标识符
     * @param body 请求体: uuid 请求信息封装 alias 请求信息封装
     * @return 设备按键列表响应
     */
    @Operation(path = "/device/v2/ir/{device_id}", method = "PUT")
    fun updateIRDevice(
        @Parameter(name = "device_id", location = "path") deviceId: String,
        body: IrDeviceModifyRequest
    ): Any

    /**
     * 更新IR设备状态
     *
     * @param deviceId 设备唯一标识符
     * @param body 请求体: uuid 请求信息封装 state 请求信息封装
     * @return 设备按键列表响应
     */
    @Operation(path = "/device/v2/ir/{device_id}", method = "PUT")
    fun updateIRDeviceState(
        @Parameter(name = "device_id", location = "path") deviceId: String,
        body: IrDeviceStateRequest
    ): Any

    /**
     * 获取IR设备列表
     */
    @Operation(path = "/device/v2/ir/{device_id}", method = "GET")
    fun fetchIRDevices(
        @Parameter(name = "device_id", location = "path") deviceId: String
    ): Any

    /**
     * 删除IR遥控器
     */
    @Operation(path = "/device/v2/ir/{device_id}", method = "DELETE")
    fun deleteIRDevice(
        @Parameter(name = "device_id", location = "path") deviceId: String,
        body: IrDeviceDeleteRequest
    ): Any


    /**
     * 发送IR设备按键
     *
     * @param deviceId 设备唯一标识符
     * @param body 请求体: operation:具体动作行为  hxd ： hxd 命令 learned 自学习命令
     * @return 设备按键列表响应
     */
    @Operation(path = "/device/v2/ir/{device_id}/send", method = "POST")
    fun emitIRRemoteDeviceKey(
        @Parameter(name = "device_id", location = "path") deviceId: String,
        body: IrDeviceRemoteKeyRequest
    ): Any

    @Operation(path = "/device/v2/ir/{device_id}/mode", method = "PUT")
    fun sendIRMode(
        @Parameter(name = "device_id", location = "path") deviceId: String,
        body: IrDeviceRemoteKeyRequest
    ): Any

    @Operation(path = "/device/v2/ir/hub3_match_ir_code", method = "POST")
    fun matchIrCode(
        body: IrMatchCodeRequest
    ): Any

}

