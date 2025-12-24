package co.candyhouse.server

import co.candyhouse.app.BuildConfig
import co.candyhouse.app.tabs.account.CHUserKey
import com.amazonaws.mobileconnectors.apigateway.annotation.Operation
import com.amazonaws.mobileconnectors.apigateway.annotation.Parameter
import com.amazonaws.mobileconnectors.apigateway.annotation.Service

@Service(endpoint = BuildConfig.ch_user_server)
internal interface CHLoginAPIClient {

    @Operation(path = "/device", method = "POST")
    fun updateKeys(
        @Parameter(name = "appidentifyid", location = "header") identifyId: String,
        body: List<CHUserKey>
    ): Array<CHUserKey>

    @Operation(path = "/device", method = "PUT")
    fun putKey(
        @Parameter(name = "appidentifyid", location = "header") identifyId: String,
        body: CHUserKey
    ): Any

    @Operation(path = "/device/list", method = "GET")
    fun getDevicesList(
        @Parameter(name = "appidentifyid", location = "header") identifyId: String
    ): Array<CHUserKey>

    @Operation(path = "/device", method = "DELETE")
    fun removeKey(
        @Parameter(name = "appidentifyid", location = "header") identifyId: String,
        body: String
    ): Any

    @Operation(path = "/friend", method = "POST")
    fun addFriend(
        @Parameter(name = "appidentifyid", location = "header") identifyId: String,
        body: String
    ): Any

    @Operation(path = "/friend/token", method = "POST")
    fun uploadDeviceToken(
        @Parameter(name = "appidentifyid", location = "header") identifyId: String,
        body: Any
    ): Any

    @Operation(path = "/web_route", method = "POST")
    fun getWebUrlByScene(
        @Parameter(name = "appidentifyid", location = "header") identifyId: String,
        body: ScenePayload
    ): Any
}

data class ScenePayload(
    val scene: String,
    val token: String? = null,
    val extInfo: Map<String, String>? = null
)