package co.candyhouse.app.connecteddevice

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import co.candyhouse.app.SesameApp
import co.candyhouse.sesame.utils.L
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

object AutoUnlockGeofenceManager {
    private val generation = AtomicInteger()
    private var lastConfiguration: String? = null
    fun hasPermission(context: Context) = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            (Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED)

    private fun pending(context: Context) = PendingIntent.getBroadcast(
        context, 0, Intent(context, AutoUnlockGeofenceReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
    )

    fun sync(context: Context) {
        val app = context.applicationContext as SesameApp
        val configuration = app.ble.allDevices().filter { app.devicePreferences.enabled("nohand", it.deviceId.toString()) }.map {
            val d = app.devicePreferences.snapshot(it.deviceId.toString()); "${it.deviceId}:${d.getDouble("latitude")}:${d.getDouble("longitude")}:${d.getDouble("radius")}"
        }.sorted().joinToString(";") + hasPermission(context)
        if (configuration == lastConfiguration) return
        lastConfiguration = configuration
        val epoch = generation.incrementAndGet()
        val client = LocationServices.getGeofencingClient(context)
        val intent = pending(context)
        val fences = if (!hasPermission(context)) emptyList() else app.ble.allDevices().filter { app.devicePreferences.enabled("nohand", it.deviceId.toString()) }.mapNotNull {
            val id = it.deviceId.toString();
            val data = app.devicePreferences.snapshot(id)
            val lat = data.getDouble("latitude");
            val lng = data.getDouble("longitude")
            if (lat == 0.0 && lng == 0.0) null else Geofence.Builder().setRequestId(id)
                .setCircularRegion(lat, lng, data.getDouble("radius").toFloat()).setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT).build()
        }.take(100)
        client.removeGeofences(intent).addOnCompleteListener {
            if (epoch != generation.get() || fences.isEmpty()) return@addOnCompleteListener
            try {
                client.addGeofences(GeofencingRequest.Builder().setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT).addGeofences(fences).build(), intent)
                    .addOnFailureListener { lastConfiguration = null; L.e("AutoUnlockGeofence", "Registration failed", it) }
            } catch (e: SecurityException) {
                L.e("AutoUnlockGeofence", "Location permission unavailable", e)
            }
        }
    }
}

class AutoUnlockGeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError() || event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_EXIT) return
        val app = context.applicationContext as SesameApp
        event.triggeringGeofences.orEmpty().forEach { if (app.devicePreferences.enabled("nohand", it.requestId)) app.devicePreferences.setEnabled("nohandg", it.requestId, true) }
        val pending = goAsync()
        app.runtimeScope.launch {
            try {
                app.ble.restoreSaved(); SesameConnectedDeviceService.sync(context)
            } finally {
                pending.finish()
            }
        }
    }
}

class AutoUnlockBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val pending = goAsync();
        val app = context.applicationContext as SesameApp
        app.runtimeScope.launch {
            try {
                app.ble.restoreSaved(); AutoUnlockGeofenceManager.sync(context)
            } finally {
                pending.finish()
            }
        }
    }
}
