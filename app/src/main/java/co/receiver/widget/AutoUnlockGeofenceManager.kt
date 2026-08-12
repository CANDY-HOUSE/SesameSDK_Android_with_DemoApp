package co.receiver.widget

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import co.candyhouse.app.tabs.devices.ssm2.getIsNOHand
import co.candyhouse.app.tabs.devices.ssm2.getNOHandLeft
import co.candyhouse.app.tabs.devices.ssm2.getNOHandRadius
import co.candyhouse.app.tabs.devices.ssm2.getNOHandRight
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.SharedPreferencesUtils
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import java.util.concurrent.atomic.AtomicInteger

object AutoUnlockGeofenceManager {
    private const val TAG = "AutoUnlockGeofence"
    private val syncGeneration = AtomicInteger(0)

    private fun pendingIntent(context: Context): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    0
                }
        return PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, AutoUnlockGeofenceReceiver::class.java),
            flags
        )
    }

    fun hasRequiredLocationPermission(context: Context): Boolean {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasBackgroundLocation = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        return hasFineLocation && hasBackgroundLocation
    }

    fun sync(context: Context, devices: List<CHDevices>) {
        if (!hasRequiredLocationPermission(context)) {
            clear(context)
            L.d(TAG, "Skip geofence sync: location permission is incomplete")
            return
        }

        val generation = syncGeneration.incrementAndGet()
        val allGeofences = devices
            .filter { it.getIsNOHand() }
            .mapNotNull { device ->
                val latitude = device.getNOHandLeft().toDouble()
                val longitude = device.getNOHandRight().toDouble()
                if (latitude == 0.0 && longitude == 0.0) {
                    return@mapNotNull null
                }
                Geofence.Builder()
                    .setRequestId(device.deviceId.toString())
                    .setCircularRegion(latitude, longitude, device.getNOHandRadius())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build()
            }
        val geofences = allGeofences.take(100)

        if (allGeofences.size > geofences.size) {
            L.d(TAG, "Only the first 100 valid auto-unlock geofences can be registered")
        }

        val client = LocationServices.getGeofencingClient(context)
        val geofencePendingIntent = pendingIntent(context)
        client.removeGeofences(geofencePendingIntent).addOnCompleteListener {
            if (generation != syncGeneration.get()) return@addOnCompleteListener
            if (geofences.isEmpty()) {
                L.d(TAG, "All auto-unlock geofences removed")
                return@addOnCompleteListener
            }

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
                .addGeofences(geofences)
                .build()
            try {
                client.addGeofences(request, geofencePendingIntent)
                    .addOnSuccessListener { L.d(TAG, "Registered ${geofences.size} geofences") }
                    .addOnFailureListener { L.d(TAG, "Failed to register geofences: ${it.message}") }
            } catch (e: SecurityException) {
                L.d(TAG, "Failed to register geofences: ${e.message}")
            }
        }
    }

    fun clear(context: Context) {
        syncGeneration.incrementAndGet()
        LocationServices.getGeofencingClient(context).removeGeofences(pendingIntent(context))
    }
}

class AutoUnlockGeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            L.d("AutoUnlockGeofence", "Geofence error: ${event.errorCode}")
            return
        }
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_EXIT) return

        val requestIds = event.triggeringGeofences.orEmpty().map { it.requestId }.toSet()
        if (requestIds.isEmpty()) return

        requestIds.forEach { deviceId ->
            SharedPreferencesUtils.preferences.edit()
                .putBoolean("nohandg$deviceId", true)
                .apply()
        }
        AutoUnlockForegroundService.start(context)
        CHDeviceManager.getCandyDevices { result ->
            result.onSuccess { state ->
                requestIds.forEach { deviceId ->
                    SesameWidgetNotificationManager.update(context, state.data, deviceId)
                }
            }
        }
    }
}

class AutoUnlockBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pendingResult = goAsync()
        CHDeviceManager.getCandyDevices { result ->
            result.onSuccess { state ->
                AutoUnlockGeofenceManager.sync(context, state.data)
            }
            pendingResult.finish()
        }
    }
}
