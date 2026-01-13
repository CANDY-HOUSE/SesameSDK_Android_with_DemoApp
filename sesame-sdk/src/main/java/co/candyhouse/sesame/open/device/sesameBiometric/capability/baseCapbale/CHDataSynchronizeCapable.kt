package co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale

import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.server.dto.AuthenticationData
import co.candyhouse.sesame.server.dto.AuthenticationDataWrapper
import co.candyhouse.sesame.server.dto.CHAuthenticationNameRequest
import co.candyhouse.sesame.utils.CHEmpty

interface CHDataSynchronizeCapable {
    fun postAuthenticationData(request: AuthenticationDataWrapper, result: CHResult<List<AuthenticationData>>)
    fun putAuthenticationData(request: AuthenticationDataWrapper, result: CHResult<CHEmpty>)
    fun deleteAuthenticationData(deleteReq: AuthenticationDataWrapper, result: CHResult<CHEmpty>)
    fun updateAuthenticationName(data: CHAuthenticationNameRequest, result: CHResult<String>)
}