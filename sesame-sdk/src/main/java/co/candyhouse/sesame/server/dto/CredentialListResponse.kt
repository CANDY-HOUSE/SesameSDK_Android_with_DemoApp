package co.candyhouse.sesame.server.dto



data class CredentialListResponse(
    val action: String,
    val op: String,
    val code: Int,
    val data: CredentialListData,
    val message: String,
    val success: Boolean
)

data class CredentialListData(
    val success: Boolean,
    val tableName: String,
    val deviceID: String,
    val items: List<AuthenticationData>,
    val count: Int
)


open class AuthenticationData {
    var credentialId = ""
    var nameUUID: String = ""
    var type: Byte = 0
    var name: String = ""
    override fun toString(): String {
        return "AuthenticationData(credentialId='$credentialId', nameUUID='$nameUUID', type=$type, name='$name')"
    }


}