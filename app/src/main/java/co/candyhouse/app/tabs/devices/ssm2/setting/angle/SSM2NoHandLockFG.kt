package co.candyhouse.app.tabs.devices.ssm2.setting.angle

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgNoHandBinding
import co.candyhouse.app.tabs.devices.ssm2.getIsNOHand
import co.candyhouse.app.tabs.devices.ssm2.getNOHandLeft
import co.candyhouse.app.tabs.devices.ssm2.getNOHandRadius
import co.candyhouse.app.tabs.devices.ssm2.getNOHandRight
import co.candyhouse.app.tabs.devices.ssm2.setIsNOHand
import co.candyhouse.app.tabs.devices.ssm2.setNOHandLeft
import co.candyhouse.app.tabs.devices.ssm2.setNOHandRadius
import co.candyhouse.app.tabs.devices.ssm2.setNOHandRight
import co.utils.getLastKnownLocation
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
    private val locationPermissionRequestCode = 1
    private val accessBackgroundLocationPermissionRequestCode = 2
    private lateinit var googleMap: GoogleMap

    override fun getViewBinder() = FgNoHandBinding.inflate(layoutInflater)

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), locationPermissionRequestCode)
        } else {
            initializeMap()
        }
    }

    @Deprecated("Deprecated in Java")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionRequestCode) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), accessBackgroundLocationPermissionRequestCode)
            }
        }
        if (requestCode == accessBackgroundLocationPermissionRequestCode) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                initializeMap()
            }
        }
    }

    private var ssmMarker: Marker? = null
    private var circle: Circle? = null
    private lateinit var mapFragment: SupportMapFragment


    override fun onResume() {
        super.onResume()

        if (::mapFragment.isInitialized) {
            mapFragment.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::mapFragment.isInitialized) {
            mapFragment.onPause()
        }

    }

    override fun onStop() {
        super.onStop()
        if (::mapFragment.isInitialized) {
            mapFragment.onStop()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mapFragment.isInitialized) {
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
            mDeviceModel.ssmLockLiveData.value?.setIsNOHand(isChecked)
            mDeviceModel.updateWidgets()
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
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        if (!isAdded) return
        googleMap = map

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
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
                }

                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 17f))
            }
        }
    }
}
