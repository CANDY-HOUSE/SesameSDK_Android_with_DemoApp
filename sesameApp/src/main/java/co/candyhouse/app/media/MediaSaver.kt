package co.candyhouse.app.media

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Receives a photo or video from Biz in base64 slices and adds it to the system gallery.
 * Slices go to a cache file first, so a long recording never sits in memory as one string.
 * Request handling runs on the main thread; only file work moves to IO.
 */
class MediaSaver(private val context: Context, private val requestPermission: suspend (String) -> Boolean) {
    private enum class Kind(val mimePrefix: String, val directory: String) {
        IMAGE("image/", Environment.DIRECTORY_PICTURES),
        VIDEO("video/", Environment.DIRECTORY_MOVIES)
    }

    class PermissionDenied : Exception("Media permission denied")

    private class Transfer(val file: File, val kind: Kind, val name: String, val mime: String) {
        val lock = Mutex()
        var closed = false
    }

    private val dir = File(context.cacheDir, "media-save")
    private val transfers = mutableMapOf<String, Transfer>()

    /** begin → { id }; append (base64 `data`) / commit / abort → null. Slices are appended in arrival order. */
    suspend fun handle(json: JSONObject): JSONObject? {
        when (json.getString("operation")) {
            "begin" -> return begin(json)
            "append" -> transfer(json).withOpen { file.appendBytes(Base64.decode(json.getString("data"), Base64.DEFAULT)) }

            "commit" -> {
                val transfer = transfer(json)
                transfers.remove(json.getString("id"))
                transfer.withOpen { closed = true; publish(this) }
            }

            "abort" -> transfers.remove(json.getString("id"))?.let { transfer ->
                transfer.lock.withLock {
                    transfer.closed = true
                    withContext(Dispatchers.IO) { transfer.file.delete() }
                }
            }

            else -> throw IllegalArgumentException("Unknown media operation")
        }
        return null
    }

    private suspend fun begin(json: JSONObject): JSONObject {
        val kind = when (json.getString("kind")) {
            "image" -> Kind.IMAGE
            "video" -> Kind.VIDEO
            else -> throw IllegalArgumentException("Unknown media kind")
        }
        val mime = json.getString("mimeType").substringBefore(';').trim().lowercase()
        require(mime.startsWith(kind.mimePrefix))
        // Android 9 and older write shared storage directly and must hold the legacy permission.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            throw PermissionDenied()
        }
        val id = UUID.randomUUID().toString()
        val file = withContext(Dispatchers.IO) {
            dir.mkdirs()
            // A page reload or killed process can leave slices behind; nothing should take this long.
            dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > STALE_MS }?.forEach { it.delete() }
            File(dir, id).apply { createNewFile() }
        }
        transfers[id] = Transfer(file, kind, displayName(json.optString("fileName"), mime), mime)
        return JSONObject().put("id", id)
    }

    private fun transfer(json: JSONObject) = requireNotNull(transfers[json.getString("id")]) { "Unknown media transfer" }

    private suspend fun <T> Transfer.withOpen(block: Transfer.() -> T): T = lock.withLock {
        check(!closed) { "Media transfer closed" }
        withContext(Dispatchers.IO) { block() }
    }

    private fun publish(transfer: Transfer) {
        try {
            check(transfer.file.length() > 0) { "Empty media" }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) publishScoped(transfer) else publishLegacy(transfer)
        } finally {
            transfer.file.delete()
        }
    }

    private fun publishScoped(transfer: Transfer) {
        val collection =
            if (transfer.kind == Kind.IMAGE) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val resolver = context.contentResolver
        val uri = requireNotNull(resolver.insert(collection, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, transfer.name)
            put(MediaStore.MediaColumns.MIME_TYPE, transfer.mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${transfer.kind.directory}/$ALBUM")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }))
        try {
            requireNotNull(resolver.openOutputStream(uri)).use { output -> transfer.file.inputStream().use { it.copyTo(output) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Throwable) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    @Suppress("DEPRECATION")
    private fun publishLegacy(transfer: Transfer) {
        val album = File(Environment.getExternalStoragePublicDirectory(transfer.kind.directory), ALBUM).apply { mkdirs() }
        val base = transfer.name.substringBeforeLast('.')
        val extension = transfer.name.substringAfterLast('.', "")
        val target = generateSequence(0) { it + 1 }
            .map { File(album, if (it == 0) transfer.name else "$base ($it).$extension") }
            .first { !it.exists() }
        try {
            transfer.file.inputStream().use { input -> FileOutputStream(target).use { input.copyTo(it) } }
        } catch (e: Throwable) {
            target.delete()
            throw e
        }
        MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(transfer.mime), null)
    }

    private fun displayName(requested: String, mime: String): String {
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: mime.substringAfter('/')
        val name = requested.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("""[\u0000-\u001f:*?"<>|]+"""), "_").trim().trimStart('.').take(120)
            .ifEmpty { "${ALBUM}_${System.currentTimeMillis()}" }
        return if (name.contains('.')) name else "$name.$extension"
    }

    private companion object {
        const val ALBUM = "Sesame"
        const val STALE_MS = 60 * 60 * 1000L
    }
}
