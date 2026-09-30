package co.candyhouse.app.connecteddevice

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.view.MotionEvent
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import co.candyhouse.app.R
import co.candyhouse.app.SesameApp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONObject
import android.R as AndroidR

/** The map is a native surface; its surrounding controls and routing belong to Biz. */
class AutoUnlockMap(private val activity: ComponentActivity, private val container: FrameLayout, private val webView: () -> WebView, private val changed: (JSONObject) -> Unit) {
    private val app get() = activity.application as SesameApp
    private var view: MapView? = null
    private var map: GoogleMap? = null
    private val touchRect = RectF()
    private var mapTopInset = 0
    private var mapGesture = false
    private var overlayWeb: WebView? = null
    private var id: String? = null
    private var pendingEnable = false
    private var previewRadius: Double? = null
    private var marker: Marker? = null
    private var circle: Circle? = null
    fun handle(json: JSONObject): JSONObject {
        val operation = json.optString("operation")
        if (operation == "close") {
            close(); return JSONObject()
        }
        val uuid = json.getString("deviceUUID")
        require(app.ble.device(uuid) != null)
        if (operation == "open") {
            if (id != uuid) {
                close(); id = uuid
                view = MapView(activity).also { v ->
                    container.addView(v, 0); v.onCreate(null); v.onStart(); v.onResume()
                    attachOverlay(v)
                    v.getMapAsync { m ->
                        if (view !== v) return@getMapAsync
                        map = m; m.uiSettings.isMapToolbarEnabled = false
                        m.setPadding(0, mapTopInset, 0, 0)
                        m.setOnMapClickListener { point ->
                            app.devicePreferences.setRegion(uuid, JSONObject().put("latitude", point.latitude).put("longitude", point.longitude))
                            draw(); sync()
                        }
                        draw(true); locate()
                    }
                }
            }
            val touch = json.getJSONObject("touchRect")
            val density = activity.resources.displayMetrics.density
            touchRect.set(
                (touch.getDouble("left") * density).toFloat(), (touch.getDouble("top") * density).toFloat(),
                ((touch.getDouble("left") + touch.getDouble("width")) * density).toFloat(), ((touch.getDouble("top") + touch.getDouble("height")) * density).toFloat()
            )
            val rect = json.getJSONObject("rect");
            val scale = activity.resources.displayMetrics.density
            mapTopInset = (touchRect.top - rect.getDouble("top") * scale).toInt().coerceAtLeast(0)
            map?.setPadding(0, mapTopInset, 0, 0)
            view?.layoutParams = FrameLayout.LayoutParams((rect.getDouble("width") * scale).toInt(), (rect.getDouble("height") * scale).toInt()).apply {
                leftMargin = (rect.getDouble("left") * scale).toInt(); topMargin = (rect.getDouble("top") * scale).toInt()
            }
        } else if (operation == "region") {
            val radius = json.getDouble("radius")
            require(radius.isFinite() && radius in 20.0..500.0)
            if (json.optBoolean("commit")) {
                app.devicePreferences.setRegion(uuid, json); previewRadius = null; draw(); sync()
            } else {
                previewRadius = radius; draw()
            }
        } else if (operation == "enable") {
            if (!json.getBoolean("value")) {
                pendingEnable = false; app.devicePreferences.setEnabled("nohand", uuid, false); app.devicePreferences.setEnabled("nohandg", uuid, false); sync()
            } else if (AutoUnlockGeofenceManager.hasPermission(activity)) {
                app.devicePreferences.setEnabled("nohand", uuid, true); sync()
            } else {
                pendingEnable = true
                if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    activity.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 14)
                } else showDisclosure()
            }
        }
        return app.devicePreferences.snapshot(uuid)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachOverlay(mapView: MapView) {
        val web = webView()
        overlayWeb = web
        web.setBackgroundColor(Color.TRANSPARENT)
        web.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) mapGesture = touchRect.contains(event.x, event.y)
            if (!mapGesture) return@setOnTouchListener false
            val forwarded = MotionEvent.obtain(event)
            forwarded.offsetLocation(-mapView.left.toFloat(), -mapView.top.toFloat())
            try {
                mapView.dispatchTouchEvent(forwarded)
            } finally {
                forwarded.recycle()
            }
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) mapGesture = false
            true
        }
    }

    private fun showDisclosure() {
        AlertDialog.Builder(activity).setTitle(R.string.auto_unlock_location_permission_title).setMessage(R.string.auto_unlock_location_permission_message)
            .setNegativeButton(AndroidR.string.cancel) { _, _ -> pendingEnable = false }
            .setOnCancelListener { pendingEnable = false }
            .setPositiveButton(R.string.auto_unlock_location_permission_open_settings) { _, _ ->
                activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${activity.packageName}")))
            }.show()
    }

    fun permissionResult() {
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            pendingEnable = false; return
        }
        locate(); if (pendingEnable) {
            if (AutoUnlockGeofenceManager.hasPermission(activity)) resume() else if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) showDisclosure() else pendingEnable =
                false
        }
    }

    fun resume() {
        view?.onResume()
        val uuid = id ?: return
        if (pendingEnable && AutoUnlockGeofenceManager.hasPermission(activity)) {
            pendingEnable = false; app.devicePreferences.setEnabled("nohand", uuid, true); sync()
        }
    }

    fun pause() {
        view?.onPause()
    }

    private fun sync() {
        AutoUnlockGeofenceManager.sync(activity); SesameConnectedDeviceService.sync(activity); id?.let { changed(app.devicePreferences.snapshot(it)) }
    }

    private fun draw(move: Boolean = false) {
        val uuid = id ?: return;
        val m = map ?: return;
        val data = app.devicePreferences.snapshot(uuid)
        val point = LatLng(data.getDouble("latitude"), data.getDouble("longitude"))
        marker?.remove(); circle?.remove()
        marker = m.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_round)).anchor(0.5f, 0.5f).position(point))
        circle = m.addCircle(CircleOptions().center(point).radius(previewRadius ?: data.getDouble("radius")).strokeWidth(0f).fillColor(0x30ff0000))
        if (move) m.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 17f))
    }

    private fun locate() {
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 14); return
        }
        map?.isMyLocationEnabled = true
        val uuid = id ?: return
        LocationServices.getFusedLocationProviderClient(activity).lastLocation.addOnSuccessListener { location ->
            if (id != uuid || location == null) return@addOnSuccessListener
            val old = app.devicePreferences.snapshot(uuid)
            if (old.getDouble("latitude") == 0.0 && old.getDouble("longitude") == 0.0) {
                app.devicePreferences.setRegion(uuid, JSONObject().put("latitude", location.latitude).put("longitude", location.longitude)); draw(true); sync()
            }
        }
    }

    fun close() {
        overlayWeb?.apply { setOnTouchListener(null); setBackgroundColor(Color.WHITE) }
        overlayWeb = null; mapGesture = false; touchRect.setEmpty(); mapTopInset = 0
        pendingEnable = false; previewRadius = null; view?.let { it.onPause(); it.onStop(); it.onDestroy(); container.removeView(it) }; view = null; map = null; id = null; marker =
            null; circle = null
    }
}
