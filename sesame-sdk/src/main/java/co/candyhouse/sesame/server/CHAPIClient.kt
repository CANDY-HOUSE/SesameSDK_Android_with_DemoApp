package co.candyhouse.sesame.server

import co.candyhouse.sesame.BuildConfig
import co.candyhouse.sesame.server.dto.CHBatteryDataReq
import co.candyhouse.sesame.server.dto.CHFcmTokenUpload
import co.candyhouse.sesame.server.dto.CHRemoveSignKeyRequest
import co.candyhouse.sesame.server.dto.CHSS2RegisterReq
import co.candyhouse.sesame.server.dto.CHSS2RegisterRes
import co.candyhouse.sesame.server.dto.CHSS2WebCMDReq
import co.candyhouse.sesame.server.dto.CHUserKey
import co.candyhouse.sesame.server.dto.ScenePayload
import co.candyhouse.sesame.server.dto.SubscriptionRequest
import com.amazonaws.mobileconnectors.apigateway.annotation.Operation
import com.amazonaws.mobileconnectors.apigateway.annotation.Parameter
import com.amazonaws.mobileconnectors.apigateway.annotation.Service

@Service(endpoint = BuildConfig.ch_server)
internal interface CHAPIClient {
    // 发送網路鑰匙
    @Operation(path = "/device", method = "POST")
    fun updateKeys(
        @Parameter(name = "appidentifyid", location = "header") identifyId: String,
        body: List<CHUserKey>
    ): Array<CHUserKey>

    // 更新網路鑰匙
    @Operation(path = "/device", method = "PUT")
    fun putKey(
        @Parameter(name = "appidentifyid", location = "header") identifyId: String,
        body: CHUserKey
    ): Any

    // 获取網路鑰匙
    @Operation(path = "/device/list", method = "GET")
    fun getDevicesList(
        @Parameter(name = "appidentifyid", location = "header") identifyId: String
    ): Array<CHUserKey>

    // 移除用戶網路鑰匙
    @Operation(path = "/device", method = "DELETE")
    fun removeKey(
        @Parameter(name = "appidentifyid", location = "header") identifyId: String,
        body: String
    ): Any

    // 新增好友
    @Operation(path = "/friend", method = "POST")
    fun addFriend(
        @Parameter(name = "appidentifyid", location = "header") identifyId: String,
        body: String
    ): Any

    // 上传用户 Token
    @Operation(path = "/friend/token", method = "POST")
    fun uploadDeviceToken(
        @Parameter(name = "appidentifyid", location = "header") identifyId: String,
        body: Any
    ): Any

    // 获取网页链接
    @Operation(path = "/web_route", method = "POST")
    fun getWebUrlByScene(
        @Parameter(name = "appidentifyid", location = "header") identifyId: String,
        body: ScenePayload
    ): Any

    // 发送IoT命令到设备
    @Operation(path = "/device/v1/iot/sesame2/{device_id}", method = "POST")
    fun ss2CommandToWM2Post(
        @Parameter(name = "device_id", location = "path") model: String?,
        body: CHSS2WebCMDReq
    ): Any

    // 注册设备（os2）
    @Operation(path = "/device/v1/sesame2/{device_id}", method = "POST")
    fun myDevicesRegisterSesame2Post(
        @Parameter(name = "device_id", location = "path") model: String?,
        body: CHSS2RegisterReq?
    ): CHSS2RegisterRes

    // 注册设备（os3）
    @Operation(path = "/device/v1/sesame5/{device_id}", method = "POST")
    fun myDevicesRegisterSesame5Post(
        @Parameter(name = "device_id", location = "path") model: String?,
        body: Any?
    ): Any

    // 上传历史记录标签
    @Operation(path = "/device/v1/sesame2/historys", method = "POST")
    fun feedHistory(body: Any): Any

    // 访客钥匙签名
    @Operation(path = "/device/v1/sesame2/sign", method = "POST")
    fun guestKeysSignPost(body: CHRemoveSignKeyRequest): String

    // 获取Hub3状态
    @Operation(path = "/device/v2/hub3/{device_id}/status", method = "GET")
    fun getHub3StatusFromIot(
        @Parameter(name = "device_id", location = "path") deviceId: String,
    ): Any

    // 生物识别数据操作 (通用)
    @Operation(path = "/device/v2/credential", method = "POST")
    fun credentialOperation(body: Any): Any

    // 订阅 SNS 主题
    @Operation(path = "/device/v1/subscribe", method = "POST")
    fun subscribeToTopic(body: SubscriptionRequest): Any

    // 上传电池数据
    @Operation(path = "/device/v2/sesame5/{device_id}/battery", method = "POST")
    fun postBatteryData(
        @Parameter(name = "device_id", location = "path") deviceID: String,
        body: CHBatteryDataReq
    ): Any

    // 移除用户 Token
    @Operation(path = "/device/v1/token", method = "DELETE")
    fun fcmTokenSignDelete(body: CHFcmTokenUpload): Any

    // 上传固件版本号
    @Operation(path = "/device/v2/sesame5/{device_id}/fwVer", method = "POST")
    fun updateDeviceFirmwareVersion(
        @Parameter(name = "device_id", location = "path") deviceId: String,
        body: Map<String, Any>
    ): Any
}