package co.candyhouse.sesame.server.dto

/**
 * 
 *
 * @author frey  on 2026/6/29
 */
data class FirmwareZipUrlResponse(
    var ok: Boolean = false,
    var code: String? = null,
    var message: String? = null,
    var productType: Int? = null,
    var prefix: String? = null,
    var firmwareName: String? = null,
    var fileName: String? = null,
    var zipUrl: String? = null,
    var s3Key: String? = null
)