package co.candyhouse.server

import co.candyhouse.app.BuildConfig
import co.candyhouse.app.tabs.account.CHUserKey
import com.amazonaws.mobileconnectors.apigateway.annotation.Operation
import com.amazonaws.mobileconnectors.apigateway.annotation.Service

@Service(endpoint = BuildConfig.ch_user_server) internal interface CHLoginAPIClient {

    @Operation(path = "/device", method = "POST")
    fun updateKeys(body: List<CHUserKey>): Array<CHUserKey>

    @Operation(path = "/device", method = "PUT")
    fun putKey(body: CHUserKey): Any

    @Operation(path = "/device/list", method = "GET")
    fun getDevicesList(): Array<CHUserKey>

    @Operation(path = "/device", method = "DELETE")
    fun removeKey(body: String): Any

    @Operation(path = "/friend", method = "POST")
    fun addFriend(body: String): Any

    @Operation(path = "/friend/token", method = "POST")
    fun uploadDeviceToken(body: Any): Any

    @Operation(path = "/web_route", method = "POST")
    fun getWebUrlByScene(body: ScenePayload): Any
}

data class ScenePayload(
    val scene: String,
    val token: String? = null,
    val extInfo: Map<String, String>? = null
)