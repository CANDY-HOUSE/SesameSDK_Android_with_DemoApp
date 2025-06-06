package co.candyhouse.sesame.open.device.sesameBiometric.capability.baseCapbale

import co.candyhouse.sesame.open.CHResult
import co.candyhouse.sesame.server.dto.AuthenticationDataWrapper
import co.candyhouse.sesame.server.dto.CHEmpty
import co.candyhouse.sesame.server.dto.AuthenticationData

interface CHDataSynchronizeCapable {
    fun postAuthenticationData(request: AuthenticationDataWrapper, result: CHResult<List<AuthenticationData>>)
    fun putAuthenticationData(request: AuthenticationDataWrapper, result: CHResult<CHEmpty>)
    fun deleteAuthenticationData(deleteReq: AuthenticationDataWrapper, result: CHResult<CHEmpty>)
}