package co.candyhouse.sesame.server

class CHApiException(
    val statusCode: Int,
    val errorMessage: String?,
    cause: Throwable? = null
) : Exception(errorMessage, cause)
