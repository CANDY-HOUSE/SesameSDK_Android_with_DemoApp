package co.candyhouse.sesame.server.dto

internal data class CHSS2RegisterReq(var s1: CHSS2RegisterReqSig1)
internal data class CHSS2RegisterReqSig1(var ak: String, var n: String, var e: String, var t: String)
internal data class CHOS3RegisterReq(var t: String, var pk: String)
