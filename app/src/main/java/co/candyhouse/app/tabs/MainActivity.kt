/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.candyhouse.app.tabs

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import co.candyhouse.app.AWSConfig
import co.candyhouse.app.tabs.account.login.LoginFragment
import co.candyhouse.sesame.ble.CHBleManager
import co.candyhouse.app.R
import co.candyhouse.app.tabs.account.MeFG
import co.candyhouse.app.tabs.devices.DeviceListFG
import co.candyhouse.app.tabs.friends.FriendsFG
import co.candyhouse.sesame.server.CHAccountManager
import co.utils.L
import co.utils.SharedPreferencesUtils
import com.amazonaws.mobile.client.*
import com.amazonaws.mobile.config.AWSConfiguration
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import pub.devrel.easypermissions.EasyPermissions
import java.util.concurrent.CountDownLatch

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    lateinit var currentNavController: LiveData<NavController>

    companion object {
        var activity: MainActivity? = null
        var nowTab = 0
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        L.d("hcia", "onCreate:")

        MainActivity.activity = this
        setContentView(R.layout.activity_main)
        setupBottomNavigationBar()
        getPermissions()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

    }


    override fun onResume() {
        super.onResume()
//        L.d("hcia", "onResume 啟動掃描:" )
        CHBleManager.enableScan()
    }

    override fun onPause() {
        super.onPause()
//        L.d("hcia", "APP退到背景 斷線全部 onPause:")
        CHBleManager.disableScan()
//        CHBleManager.disconnectAll()
    }


    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun setupBottomNavigationBar() {
        val navGraphIds = listOf(
                R.navigation.list,
                R.navigation.home,
                R.navigation.account_ng
        )

        val controller = bottom_nav.setupWithNavController(//BottomNavigationView
                navGraphIds = navGraphIds,
                fragmentManager = supportFragmentManager,
                containerId = R.id.nav_host_container,
                intent = intent
        )


        // Whenever the selected controller changes, setup the action bar.
        controller.observe(this, Observer { navController ->
            currentNavController = controller
        })
        currentNavController = controller

    }

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(
                applicationContext,
                "ACCESS_FINE_LOCATION(bluetooth  nead)",  // todo i18n
                Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        CHBleManager.enableScan()
    }

    fun hideMenu() {
        bottom_nav.visibility = View.GONE
    }

    fun showMenu() {
        bottom_nav.visibility = View.VISIBLE
    }

    private fun getPermissions() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            L.d("hcia", "get PR 啟動掃描::" )
            CHBleManager.enableScan()
        } else {
            EasyPermissions.requestPermissions(
                    this, "ACCESS_FINE_LOCATION(bluetooth  nead)", 0,
                    Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun showProgress() {
        runOnUiThread {
            pBar.visibility = View.VISIBLE
        }
    }

    fun hideProgress() {
        runOnUiThread {
            pBar.visibility = View.GONE
        }
    }

}
