package co.candyhouse.sesame.server

import co.candyhouse.sesame.BuildConfig
import co.candyhouse.sesame.server.dto.AuthenticationDataWrapper
import co.candyhouse.sesame.server.dto.CHBatteryDataReq
import co.candyhouse.sesame.server.dto.CHCardNameRequest
import co.candyhouse.sesame.server.dto.CHFaceNameRequest
import co.candyhouse.sesame.server.dto.CHFcmTokenUpload
import co.candyhouse.sesame.server.dto.CHFingerPrintNameRequest
import co.candyhouse.sesame.server.dto.CHKeyBoardPassCodeNameRequest
import co.candyhouse.sesame.server.dto.CHPalmNameRequest
import co.candyhouse.sesame.server.dto.CHRemoveSignKeyRequest
import co.candyhouse.sesame.server.dto.CHSS2RegisterReq
import co.candyhouse.sesame.server.dto.CHSS2RegisterRes
import co.candyhouse.sesame.server.dto.CHSS2WebCMDReq
import co.candyhouse.sesame.server.dto.SubscriptionRequest
import com.amazonaws.mobileconnectors.apigateway.annotation.Operation
import com.amazonaws.mobileconnectors.apigateway.annotation.Parameter
import com.amazonaws.mobileconnectors.apigateway.annotation.Service

@Service(endpoint = BuildConfig.chjpserver)
internal interface CHPrivateAPIClient {

    @Operation(path = "/device/v1/iot/sesame2/{device_id}", method = "POST")
    fun ss2CommandToWM2Post(
        @Parameter(name = "device_id", location = "path") model: String?,
        body: CHSS2WebCMDReq
    ): Any

    @Operation(path = "/device/v1/sesame2/{device_id}", method = "POST")
    fun myDevicesRegisterSesame2Post(
        @Parameter(name = "device_id", location = "path") model: String?,
        body: CHSS2RegisterReq?
    ): CHSS2RegisterRes

    @Operation(path = "/device/v1/sesame5/{device_id}", method = "POST")
    fun myDevicesRegisterSesame5Post(
        @Parameter(name = "device_id", location = "path") model: String?,
        body: Any?
    ): Any

    @Operation(path = "/device/v1/sesame2/historys", method = "POST")
    fun feedHistory(body: Any): Any

    @Operation(path = "/device/v1/sesame2/sign", method = "POST")
    fun guestKeysSignPost(body: CHRemoveSignKeyRequest): String

    @Operation(path = "/device/v1/version", method = "GET")
    fun getVersion(): Any

    @Operation(path = "/device/v2/card/name", method = "PUT")
    fun setCardName(
        body: CHCardNameRequest,
    ): String

    @Operation(path = "/device/v2/fingerprint/name", method = "PUT")
    fun setFingerPrintName(
        body: CHFingerPrintNameRequest,
    ): String

    @Operation(path = "/device/v2/passcode/name", method = "PUT")
    fun setKeyBoardPassCodeName(
        body: CHKeyBoardPassCodeNameRequest,
    ): String

    @Operation(path = "/device/v2/face/name", method = "PUT")
    fun setFaceName(
        body: CHFaceNameRequest,
    ): String

    @Operation(path = "/device/v2/palm/name", method = "PUT")
    fun setPalmName(
        body: CHPalmNameRequest,
    ): String

    @Operation(path = "/device/v2/hub3/{device_id}/status", method = "GET")
    fun getHub3StatusFromIot(
        @Parameter(name = "device_id", location = "path") deviceId: String,
    ): Any

    @Operation(path = "/device/v2/hub3/{device_id}/firmware", method = "POST")
    fun updateHub3Firmware(
        @Parameter(name = "device_id", location = "path") deviceId: String,
    ): Any

    @Operation(path = "/device/v2/credential", method = "POST")
    fun postCredentialListToServer(
        credentialListRequest: AuthenticationDataWrapper
    ): Any

    @Operation(path = "/device/v2/credential", method = "POST")
    fun deleteCredentialInfo(deleteRequest: AuthenticationDataWrapper): Any

    @Operation(path = "/device/v1/subscribe", method = "POST")
    fun subscribeToTopic(body: SubscriptionRequest): Any

    @Operation(path = "/device/v2/sesame5/{device_id}/battery", method = "POST")
    fun postBatteryData(
        @Parameter(name = "device_id", location = "path") deviceID: String,
        body: CHBatteryDataReq
    ): Any

    @Operation(path = "/device/v1/token", method = "DELETE")
    fun fcmTokenSignDelete(body: CHFcmTokenUpload): Any

}