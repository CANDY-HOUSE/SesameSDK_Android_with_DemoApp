package co.candyhouse.server

import co.candyhouse.server.dto.IrDeviceDeleteRequest
import co.candyhouse.sesame.BuildConfig
import com.amazonaws.mobileconnectors.apigateway.annotation.Operation
import com.amazonaws.mobileconnectors.apigateway.annotation.Parameter
import com.amazonaws.mobileconnectors.apigateway.annotation.Service

@Service(endpoint = BuildConfig.chjpserver)
internal interface CHIRAPIClient {

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

}

