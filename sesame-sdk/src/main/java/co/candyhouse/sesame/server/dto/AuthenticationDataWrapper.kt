package co.candyhouse.sesame.server.dto

import com.google.gson.annotations.SerializedName

data class AuthenticationDataWrapper(
    @SerializedName("op") var operation: String,
    @SerializedName("deviceID") val deviceID: String,
    @SerializedName("items") val credentialList: List<AuthenticationData>
) {
    override fun toString(): String {
        return "AuthenticationDataWrapper(operation='$operation', deviceID='$deviceID', credentialList=$credentialList)"
    }
}
