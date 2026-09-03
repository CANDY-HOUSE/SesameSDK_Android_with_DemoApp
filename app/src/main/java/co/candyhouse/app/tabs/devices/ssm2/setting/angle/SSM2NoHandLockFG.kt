package co.candyhouse.app.tabs.devices.ssm2.setting.angle

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgNoHandBinding
import co.candyhouse.app.ext.connecteddevice.AutoUnlockGeofenceManager
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.app.tabs.devices.ssm2.getIsNOHand
import co.candyhouse.app.tabs.devices.ssm2.getNOHandLeft
import co.candyhouse.app.tabs.devices.ssm2.getNOHandRadius
import co.candyhouse.app.tabs.devices.ssm2.getNOHandRight
import co.candyhouse.app.tabs.devices.ssm2.setIsNOHand
import co.candyhouse.app.tabs.devices.ssm2.setIsNOHandG
import co.candyhouse.app.tabs.devices.ssm2.setNOHandLeft
import co.candyhouse.app.tabs.devices.ssm2.setNOHandRadius
import co.candyhouse.app.tabs.devices.ssm2.setNOHandRight
import co.candyhouse.app.util.getLastKnownLocation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams

class SSM2NoHandLockFG : BaseDeviceFG<FgNoHandBinding>(), OnMapReadyCallback {
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var googleMap: GoogleMap
    private var pendingEnableAutoUnlock = false
    private var changingAutoUnlockSwitch = false

    override fun getViewBinder() = FgNoHandBinding.inflate(layoutInflater)

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            initializeMap()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                if (!::mapFragment.isInitialized) initializeMap()
                if (pendingEnableAutoUnlock) {
                    completePendingAutoUnlockOrRequestBackgroundLocation()
                }
            }
        }
    }

    private var ssmMarker: Marker? = null
    private var circle: Circle? = null
    private lateinit var mapFragment: SupportMapFragment



    override fun onResume() {
        super.onResume()

        completePendingAutoUnlockOrRequestBackgroundLocation(requestIfMissing = false)

        if (::mapFragment.isInitialized){
            mapFragment.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::mapFragment.isInitialized){
            mapFragment.onPause()
        }

    }

    override fun onStop() {
        super.onStop()
        if (::mapFragment.isInitialized){
            mapFragment.onStop()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mapFragment.isInitialized){
            mapFragment.onDestroy()
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkLocationPermission()
    }

    private fun initializeMap() {
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        bind.autolockSwitch.isChecked = mDeviceModel.ssmLockLiveData.value?.getIsNOHand() ?: false
        bind.autolockSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (changingAutoUnlockSwitch) return@setOnCheckedChangeListener

            if (isChecked && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                pendingEnableAutoUnlock = true
                changingAutoUnlockSwitch = true
                bind.autolockSwitch.isChecked = false
                changingAutoUnlockSwitch = false
                checkLocationPermission()
                return@setOnCheckedChangeListener
            }

            if (isChecked && !AutoUnlockGeofenceManager.hasRequiredLocationPermission(requireContext())) {
                pendingEnableAutoUnlock = true
                changingAutoUnlockSwitch = true
                bind.autolockSwitch.isChecked = false
                changingAutoUnlockSwitch = false
                completePendingAutoUnlockOrRequestBackgroundLocation()
                return@setOnCheckedChangeListener
            }

            mDeviceModel.ssmLockLiveData.value?.let { device ->
                device.setIsNOHand(isChecked)
                if (!isChecked) device.setIsNOHandG(false)
            }
            mDeviceModel.updateAutoUnlock()
        }

        bind.rangeBar.setProgress(mDeviceModel.ssmLockLiveData.value?.getNOHandRadius() ?: 0f)
        bind.rangeBar.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams) {
                circle?.radius = seekParams.progressFloat.toDouble()
            }

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {}

            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
                circle?.radius = seekBar.progress.toDouble()
                mDeviceModel.ssmLockLiveData.value?.setNOHandRadius(seekBar.progress.toFloat())
                mDeviceModel.updateAutoUnlock()
            }
        }
    }

    private fun completePendingAutoUnlockOrRequestBackgroundLocation(requestIfMissing: Boolean = true) {
        if (!pendingEnableAutoUnlock) return
        if (!AutoUnlockGeofenceManager.hasRequiredLocationPermission(requireContext())) {
            if (requestIfMissing) {
                (activity as? MainActivity)?.requestAutoUnlockBackgroundPermissionIfNeeded()
            }
            return
        }

        pendingEnableAutoUnlock = false
        mDeviceModel.ssmLockLiveData.value?.let { device ->
            device.setIsNOHand(true)
            changingAutoUnlockSwitch = true
            bind.autolockSwitch.isChecked = true
            changingAutoUnlockSwitch = false
            mDeviceModel.updateAutoUnlock()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        if (!isAdded) return
        googleMap = map

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        }

        getLastKnownLocation(requireContext()) { locationState ->
            if (!isAdded) return@getLastKnownLocation

            locationState.getOrNull()?.data?.let { location ->
                if (!isAdded) return@let

                val lockLiveData = mDeviceModel.ssmLockLiveData.value ?: return@let

                if (lockLiveData.getNOHandLeft() == 0f) {
                    lockLiveData.setNOHandLeft(location.latitude.toFloat())
                    lockLiveData.setNOHandRight(location.longitude.toFloat())
                }

                val lockPosition = LatLng(lockLiveData.getNOHandLeft().toDouble(), lockLiveData.getNOHandRight().toDouble())

                ssmMarker = googleMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_round)).anchor(0.5f, 0.5f).position(lockPosition).title(getString(R.string.Sesame)))

                val circleOptions = CircleOptions().apply {
                    center(lockPosition)
                    radius(lockLiveData.getNOHandRadius().toDouble())
                    fillColor(0x30ff0000)
                    strokeWidth(0f)
                }
                circle = googleMap.addCircle(circleOptions)

                googleMap.setOnMapClickListener { latLng ->
                    ssmMarker?.position = latLng
                    circle?.center = latLng
                    lockLiveData.setNOHandLeft(latLng.latitude.toFloat())
                    lockLiveData.setNOHandRight(latLng.longitude.toFloat())
                    mDeviceModel.updateAutoUnlock()
                }

                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 17f))
            }
        }
    }
}
