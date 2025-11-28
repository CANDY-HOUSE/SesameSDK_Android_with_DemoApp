package co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale

import co.candyhouse.sesame.open.CHAccountManager
import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.open.CHResultState
import co.candyhouse.sesame.server.dto.AuthenticationData
import co.candyhouse.sesame.server.dto.AuthenticationDataWrapper
import co.candyhouse.sesame.server.dto.CHAuthenticationNameRequest
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.server.dto.CredentialListResponse
import co.candyhouse.sesame.utils.L
import com.google.gson.Gson

class CHDataSynchronizeCapableImpl : CHDataSynchronizeCapable {

    override fun postAuthenticationData(request: AuthenticationDataWrapper, result: CHResult<List<AuthenticationData>>) {
        request.operation += "_post"
        CHAccountManager.postCredentialListToServer(request) { it ->
            it.onSuccess {
                val res = it.data
                val jsonString = Gson().toJson(res)
                val responses = Gson().fromJson(jsonString, CredentialListResponse::class.java)
                result.invoke(Result.success(CHResultState.CHResultStateNetworks(responses.data.items)))
            }
            it.onFailure {
                L.d("CHFaceCapableImpl", "Error: ${it.message}")
            }
        }
    }

    override fun putAuthenticationData(request: AuthenticationDataWrapper, result: CHResult<CHEmpty>) {
        request.operation += "_put"
        CHAccountManager.postCredentialListToServer(request) { it ->
            it.onSuccess {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
            it.onFailure {
                L.d("CHFaceCapableImpl", "Error: ${it.message}")
            }
        }
    }

    override fun deleteAuthenticationData(deleteReq: AuthenticationDataWrapper, result: CHResult<CHEmpty>) {
        deleteReq.operation += "_delete"
        CHAccountManager.deleteCredentialInfo(deleteReq) { it ->
            it.onSuccess {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(CHEmpty())))
            }
            it.onFailure {
                L.d("CHFaceCapableImpl", "Error: ${it.message}")
                result.invoke(Result.failure(it))
            }
        }
    }

    override fun updateAuthenticationName(
        data: CHAuthenticationNameRequest,
        result: CHResult<String>
    ) {
        val authData = when (data) {
            is CHAuthenticationNameRequest.Card -> data.request
            is CHAuthenticationNameRequest.Face -> data.request
            is CHAuthenticationNameRequest.FingerPrint -> data.request
            is CHAuthenticationNameRequest.Palm -> data.request
            is CHAuthenticationNameRequest.KeyBoardPassCode -> data.request
        }

        CHAccountManager.updateAuthenticationName(authData) { it ->
            it.onSuccess {
                result.invoke(Result.success(CHResultState.CHResultStateBLE(it.data)))
            }
            it.onFailure {
                result.invoke(Result.failure(it))
            }
        }
    }
}