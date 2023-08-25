package co.candyhouse.app.base

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import co.candyhouse.app.R
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.tabs.devices.model.CHUserViewModel
//import co.candyhouse.app.tabs.devices.model.CHUserViewModel
//import co.candyhouse.app.tabs.setupWithNavController
import co.candyhouse.sesame.open.CHBleManager
import co.utils.L
import com.google.android.material.bottomnavigation.BottomNavigationView
import pub.devrel.easypermissions.EasyPermissions
import androidx.navigation.ui.setupActionBarWithNavController

open class BaseActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    val deviceViewModel by lazy { ViewModelProvider(this).get(CHDeviceViewModel::class.java) }
    val userViewModel by lazy { ViewModelProvider(this).get(CHUserViewModel::class.java) }
    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getPermissions()
        setContentView(R.layout.activity_main)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_container) as NavHostFragment
        navController = navHostFragment.navController


    }

    private fun getPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)) {
                CHBleManager.enableScan {}
            } else {
                L.d("hcia", "Build.VERSION.SDK_INT:" + Build.VERSION.SDK_INT)
                L.d("hcia", "Build.VERSION_CODES.S:" + Build.VERSION_CODES.S)
                L.d("hcia", "(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S):" + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S))
                    EasyPermissions.requestPermissions(this, getString(R.string.launching_why_need_location_permission), 0, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_ADMIN)) {
                CHBleManager.enableScan {}
            } else {
                L.d("hcia", "Build.VERSION.SDK_INT:" + Build.VERSION.SDK_INT)
                L.d("hcia", "Build.VERSION_CODES.S:" + Build.VERSION_CODES.S)
                L.d("hcia", "(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S):" + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S))
                    EasyPermissions.requestPermissions(this, getString(R.string.launching_why_need_location_permission), 0, Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.BLUETOOTH_ADMIN)

            }
        }

    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        L.d("hcia", "onPermissionsDenied requestCode:" + requestCode)
        L.d("hcia", "perms:" + perms)
        Toast.makeText(applicationContext, getString(R.string.launching_why_need_location_permission), Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        CHBleManager.enableScan {}

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }
}

fun BottomNavigationView.setPage(index: Int) {
//    L.d("hcia", "index:" + index)
    selectedItemId = menu.getItem(index).itemId
    menu.getItem(index).isChecked = true
}