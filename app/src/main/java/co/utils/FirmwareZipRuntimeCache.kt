package co.utils

import android.content.Context
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.dto.FirmwareZipUrlResponse
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 
 *
 * @author frey on 2026/6/29
 */

object FirmwareDir {
    const val PROD = "prod"
    const val DEV = "dev"
}

class FirmwareException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

private object FirmwareZipRuntimeCache {

    private val cachedFiles = ConcurrentHashMap<String, File>()

    suspend fun getFirmwarePath(
        context: Context,
        zipUrl: String,
        fileName: String
    ): String = withContext(Dispatchers.IO) {
        cachedFiles[zipUrl]?.let { cachedFile ->
            if (cachedFile.exists() && cachedFile.length() > 0L) {
                return@withContext cachedFile.absolutePath
            }

            cachedFiles.remove(zipUrl, cachedFile)
        }

        val cacheDir = File(context.cacheDir, "firmware_runtime")

        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        cacheDir.listFiles { file ->
            file.name.endsWith(".download")
        }?.forEach { file ->
            runCatching { file.delete() }
        }

        val safeFileName = sanitizeFileName(fileName)
        val targetFile = File(cacheDir, safeFileName)
        val tempFile = File(cacheDir, "$safeFileName.download")

        try {
            download(
                zipUrl = zipUrl,
                outputFile = tempFile
            )

            if (targetFile.exists()) {
                targetFile.delete()
            }

            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            if (!targetFile.exists() || targetFile.length() <= 0L) {
                throw IOException("Downloaded firmware file is empty")
            }

            cachedFiles[zipUrl] = targetFile

            L.d(
                "firmware",
                "Download firmware success, url:$zipUrl, path:${targetFile.absolutePath}, size:${targetFile.length()}"
            )

            targetFile.absolutePath
        } catch (e: CancellationException) {
            runCatching { tempFile.delete() }
            runCatching { targetFile.delete() }
            throw e
        } catch (e: Exception) {
            runCatching { tempFile.delete() }
            runCatching { targetFile.delete() }

            L.e("firmware", "Download firmware failed, url:$zipUrl", e)

            throw FirmwareException("Download firmware failed", e)
        }
    }

    private fun download(
        zipUrl: String,
        outputFile: File
    ) {
        val connection = URL(zipUrl).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.instanceFollowRedirects = true
            connection.connect()

            val responseCode = connection.responseCode

            if (responseCode !in 200..299) {
                throw IOException("Download failed, httpCode:$responseCode")
            }

            val contentLength = getContentLengthCompat(connection)

            connection.inputStream.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!outputFile.exists() || outputFile.length() <= 0L) {
                throw IOException("Downloaded firmware file is empty")
            }

            if (contentLength > 0L && outputFile.length() != contentLength) {
                throw IOException(
                    "Downloaded firmware size mismatch, expected:$contentLength, actual:${outputFile.length()}"
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun sanitizeFileName(fileName: String): String {
        return fileName
            .substringAfterLast("/")
            .substringBefore("?")
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

    private fun getContentLengthCompat(connection: HttpURLConnection): Long {
        val headerLength = connection.getHeaderField("Content-Length")
            ?.toLongOrNull()

        if (headerLength != null && headerLength >= 0L) {
            return headerLength
        }

        val length = connection.contentLength

        return if (length >= 0) {
            length.toLong()
        } else {
            -1L
        }
    }
}

suspend fun getFirmwareZipUrlSuspend(
    productType: Int,
    deviceId: String,
    firmwareDir: String,
): FirmwareZipUrlResponse {
    return suspendCancellableCoroutine { continuation ->
        CHAPIClientBiz.getFirmwareZipUrl(
            productType = productType,
            deviceId = deviceId,
            firmwareDir = firmwareDir
        ) api@{ result ->
            if (!continuation.isActive) {
                return@api
            }

            result.onSuccess { state ->
                val response = state.data

                if (response.ok) {
                    continuation.resume(response)
                } else {
                    val message = response.message ?: "Get firmware zip url failed"

                    L.e(
                        "firmware",
                        "Firmware zip url failed, productType:$productType, deviceId:$deviceId, code:${response.code}, message:$message"
                    )

                    continuation.resumeWithException(
                        FirmwareException(message)
                    )
                }
            }

            result.onFailure { throwable ->
                L.e(
                    "firmware",
                    "Request firmware zip url failed, productType:$productType, deviceId:$deviceId",
                    throwable
                )

                continuation.resumeWithException(
                    FirmwareException(
                        throwable.message ?: "Request firmware zip url failed",
                        throwable
                    )
                )
            }
        }
    }
}

suspend fun CHDevices.getFirmwarePath(
    context: Context,
    firmwareDir: String = FirmwareDir.PROD
): String = withContext(Dispatchers.IO) {
    val type = productModel.productType()
    val deviceIdUpper = deviceId.toString().uppercase()

    val response = getFirmwareZipUrlSuspend(
        productType = type,
        deviceId = deviceIdUpper,
        firmwareDir = firmwareDir
    )

    val zipUrl = response.zipUrl?.takeIf { it.isNotBlank() }
        ?: throw FirmwareException("Firmware zip url is empty")

    val fileName = response.fileName?.takeIf { it.isNotBlank() }
        ?: throw FirmwareException("Firmware fileName is empty")

    L.d(
        "firmware",
        "Firmware zip url success, productModel:$productModel, productType:$type, s3Key:${response.s3Key}, deviceId:$deviceIdUpper, firmwareName:${response.firmwareName}, zipUrl:$zipUrl"
    )

    FirmwareZipRuntimeCache.getFirmwarePath(
        context = context,
        zipUrl = zipUrl,
        fileName = fileName
    )
}