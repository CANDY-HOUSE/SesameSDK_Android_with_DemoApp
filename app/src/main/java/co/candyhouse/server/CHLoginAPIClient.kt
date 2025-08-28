package co.candyhouse.server

import co.candyhouse.app.BuildConfig
import co.candyhouse.sesame.server.dto.CHUserKey
import com.amazonaws.mobileconnectors.apigateway.annotation.Operation
import com.amazonaws.mobileconnectors.apigateway.annotation.Parameter
import com.amazonaws.mobileconnectors.apigateway.annotation.Service

@Service(endpoint = BuildConfig.ch_user_server) internal interface CHLoginAPIClient {

    @Operation(path = "/device", method = "POST")
    fun updateKeys(body: List<CHUserKey>): Array<CHUserKey>

    @Operation(path = "/device/infor", method = "POST")
    fun putKeyInfor(body: CHDeviceInfor): Any

    @Operation(path = "/device", method = "PUT")
    fun putKey(body: CHUserKey): Any

    @Operation(path = "/device/list", method = "GET")
    fun getDevicesList(): Array<CHUserKey>

    @Operation(path = "/device", method = "DELETE")
    fun removeKey(body: String): Any

    @Operation(path = "/friend", method = "DELETE")
    fun removeFriend(body: String): Any

    @Operation(path = "/friend", method = "GET")
    fun getFriends(@Parameter(name = "l", location = "query") limit: Int
        , @Parameter(name = "p", location = "query") last: String?
    ): Array<CHUser>

    @Operation(path = "/friend", method = "POST")
    fun addFriend(body: String): Any
    @Operation(path = "/device/member", method = "GET")
    fun getFriendsWithDevice(@Parameter(name = "device_id", location = "query") deviceID: String, @Parameter(name = "a", location = "query") timeSign: String): Array<CHUser>

    @Operation(path = "/friend/device", method = "GET")
    fun deviceWithFriend(@Parameter(name = "friend_id", location = "query") tk: String): Array<CHUserKey>

    @Operation(path = "/friend/device", method = "DELETE")
    fun removeFriendDevice(body: Any): Any

    @Operation(path = "/friend/device", method = "POST")
    fun addFriendDevice(body: Any): Any

    @Operation(path = "/friend/token", method = "POST")
    fun uploadDeviceToken(body: Any): Any
}

data class CHUserKeyFriendID(val key: CHUserKey, val sub: String)
data class CHDeviceIDFriendID(val deviceUUID: String, val sub: String)
data class CHDeviceInfor(var deviceUUID: String, val deviceModel: String, val longitude: String, val latitude: String)
data class CHUser(var sub: String, val email: String, var nickname: String?, var keyLevel: Int?, var gtag: String?)
