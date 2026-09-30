package co.candyhouse.app.ble

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

/** One current location fix using existing permission; Biz owns when and where it is uploaded. */
object DeviceLocation {
    suspend fun current(context: Context): JSONObject? {
        val granted = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (!granted) return null
        return withTimeoutOrNull(15000.milliseconds) {
            suspendCancellableCoroutine { continuation ->
                val cancellation = CancellationTokenSource()
                continuation.invokeOnCancellation { cancellation.cancel() }
                try {
                    LocationServices.getFusedLocationProviderClient(context)
                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
                        .addOnCompleteListener { task ->
                            if (continuation.isActive) {
                                val location = if (task.isSuccessful) task.result else null
                                continuation.resume(location?.let { JSONObject().put("longitude", it.longitude).put("latitude", it.latitude) })
                            }
                        }
                } catch (_: SecurityException) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }
    }
}
