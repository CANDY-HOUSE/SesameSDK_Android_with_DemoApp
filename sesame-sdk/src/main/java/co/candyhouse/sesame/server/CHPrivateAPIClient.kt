package co.candyhouse.sesame.server

import co.candyhouse.sesame.server.dto.AuthenticationDataWrapper
import co.candyhouse.sesame.server.dto.CHCardNameRequest
import co.candyhouse.sesame.server.dto.CHFaceNameRequest
import co.candyhouse.sesame.server.dto.CHFcmTokenUpload
import co.candyhouse.sesame.server.dto.CHFingerPrintNameRequest
import co.candyhouse.sesame.server.dto.CHGuestKey
import co.candyhouse.sesame.server.dto.CHGuestKeyCut
import co.candyhouse.sesame.server.dto.CHHistoryEventV2
import co.candyhouse.sesame.server.dto.CHKeyBoardPassCodeNameRequest
import co.candyhouse.sesame.server.dto.CHModifyGuestKeyRequest
import co.candyhouse.sesame.server.dto.CHPalmNameRequest
import co.candyhouse.sesame.server.dto.CHRemoveGuestKeyRequest
import co.candyhouse.sesame.server.dto.CHRemoveSignKeyRequest
import co.candyhouse.sesame.server.dto.CHSS2Infor
import co.candyhouse.sesame.server.dto.CHSS2RegisterReq
import co.candyhouse.sesame.server.dto.CHSS2RegisterRes
import co.candyhouse.sesame.server.dto.CHSS2WebCMDReq
import co.candyhouse.sesame.server.dto.CHcfp
import co.candyhouse.sesame2.BuildConfig
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

    @Operation(path = "/device/v1/iot/sesame2/{device_id}", method = "GET")
    fun sdkAuthIOT(
        @Parameter(name = "device_id", location = "path") model: String?,
        @Parameter(name = "a", location = "query") a: String
    ): Any

    @Operation(path = "/device/v1/sesame2/{device_id}", method = "PUT")
    fun ss2UploadFwVersion(
        @Parameter(name = "device_id", location = "path") model: String?,
        body: CHSS2Infor
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

    @Operation(path = "/device/v2/sesame2/{device_id}/history", method = "GET")
    fun getHistory(
        @Parameter(name = "device_id", location = "path") deviceId: String,
        @Parameter(name = "cursor", location = "query") cursor: Long?,
        @Parameter(name = "lg", location = "query") lg: Int,
        @Parameter(name = "a", location = "query") a: String,
        @Parameter(name = "subUUID", location = "query") subUUID: String?
    ): CHHistoryEventV2

    @Operation(path = "/device/v1/sesame2/{device_id}/guestkey", method = "POST")
    fun guestKeyPost(
        @Parameter(name = "device_id", location = "path") deviceId: String,
        body: CHGuestKey
    ): String

    @Operation(path = "/device/v1/sesame2/{device_id}/guestkeys", method = "GET")
    fun guestKeysGet(
        @Parameter(name = "device_id", location = "path") deviceId: String,
        @Parameter(name = "a", location = "query") a: String
    ): Array<CHGuestKeyCut>

    @Operation(path = "/device/v1/sesame2/{device_id}/guestkey", method = "DELETE")
    fun guestKeysDelete(
        @Parameter(name = "device_id", location = "path") deviceId: String,
        body: CHRemoveGuestKeyRequest
    ): Any

    @Operation(path = "/device/v1/sesame2/{device_id}/guestkey", method = "PUT")
    fun guestKeysMotify(
        @Parameter(name = "device_id", location = "path") deviceId: String,
        body: CHModifyGuestKeyRequest
    ): Any

    @Operation(path = "/device/v1/sesame2/sign", method = "POST")
    fun guestKeysSignPost(body: CHRemoveSignKeyRequest): String

    @Operation(path = "/device/v1/token", method = "GET")
    fun fcmTokenGet(
        @Parameter(name = "deviceId", location = "query") deviceId: String,
        @Parameter(name = "deviceToken", location = "query") fcmToken: String,
    ): Any

    @Operation(path = "/device/v1/token", method = "POST")
    fun fcmTokenSignPost(body: CHFcmTokenUpload): Any

    @Operation(path = "/device/v1/token", method = "DELETE")
    fun fcmTokenSignDelete(body: CHFcmTokenUpload): Any

    @Operation(path = "/device/v1/version", method = "GET")
    fun getVersion(): Any

    @Operation(path = "/device/v1/cfp", method = "POST")
    fun postCfp(body: CHcfp): Any

    @Operation(path = "/device/v2/opensensor/{device_id}/history", method = "GET")
    fun openSensorHistoryGet(
        @Parameter(name = "device_id", location = "path") deviceId: String
    ): Any

    @Operation(path = "/device/v2/card/name", method = "PUT")
    fun setCardName(
        body: CHCardNameRequest,
    ): String

    @Operation(path = "/device/v2/card/name", method = "GET")
    fun getCardName(
        @Parameter(name = "cardID", location = "query") cardID: String,
        @Parameter(name = "cardNameUUID", location = "query") cardNameUUID: String,
        @Parameter(name = "subUUID", location = "query") subUUID: String,
        @Parameter(name = "stpDeviceUUID", location = "query") stpDeviceUUID: String,
    ): String

    @Operation(path = "/device/v2/fingerprint/name", method = "PUT")
    fun setFingerPrintName(
        body: CHFingerPrintNameRequest,
    ): String

    @Operation(path = "/device/v2/fingerprint/name", method = "GET")
    fun getFingerPrintName(
        @Parameter(name = "fingerPrintID", location = "query") fingerPrintID: String,
        @Parameter(name = "fingerPrintNameUUID", location = "query") fingerPrintNameUUID: String,
        @Parameter(name = "subUUID", location = "query") subUUID: String,
        @Parameter(name = "stpDeviceUUID", location = "query") stpDeviceUUID: String,
    ): String

    @Operation(path = "/device/v2/passcode/name", method = "PUT")
    fun setKeyBoardPassCodeName(
        body: CHKeyBoardPassCodeNameRequest,
    ): String

    @Operation(path = "/device/v2/passcode/name", method = "GET")
    fun getKeyBoardPassCodeName(
        @Parameter(name = "keyBoardPassCode", location = "query") keyBoardPassCode: String,
        @Parameter(name = "keyBoardPassCodeNameUUID", location = "query") keyBoardPassCodeNameUUID: String,
        @Parameter(name = "subUUID", location = "query") subUUID: String,
        @Parameter(name = "stpDeviceUUID", location = "query") stpDeviceUUID: String,
    ): String

    @Operation(path = "/device/v2/face/name", method = "PUT")
    fun setFaceName(
        body: CHFaceNameRequest,
    ): String

    @Operation(path = "/device/v2/face/name", method = "GET")
    fun getFaceName(
        @Parameter(name = "faceID", location = "query") faceID: String,
        @Parameter(name = "faceNameUUID", location = "query") faceNameUUID: String,
        @Parameter(name = "subUUID", location = "query") subUUID: String,
        @Parameter(name = "stpDeviceUUID", location = "query") stpDeviceUUID: String,
    ): String

    @Operation(path = "/device/v2/palm/name", method = "PUT")
    fun setPalmName(
        body: CHPalmNameRequest,
    ): String

    @Operation(path = "/device/v2/palm/name", method = "GET")
    fun getPalmName(
        @Parameter(name = "palmID", location = "query") palmID: String,
        @Parameter(name = "palmNameUUID", location = "query") palmNameUUID: String,
        @Parameter(name = "subUUID", location = "query") subUUID: String,
        @Parameter(name = "stpDeviceUUID", location = "query") stpDeviceUUID: String,
    ): String

    @Operation(path = "/device/v2/touchpro/{device_id}/passcode", method = "POST")
    fun sendKeyBoardPassCodeDataGetCmd(
        @Parameter(name = "device_id", location = "path") deviceId: String
    ): Any

    @Operation(path = "/device/v2/touchpro/{device_id}/passcode", method = "GET")
    fun getKeyBoardPassCodeDataFromIot(
        @Parameter(name = "device_id", location = "path") deviceId: String,
        @Parameter(name = "request_id", location = "query") requestId: String
    ): Any

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
    fun deleteCredentialInfo(deleteRequest: AuthenticationDataWrapper):Any

}