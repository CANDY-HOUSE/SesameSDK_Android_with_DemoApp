package co.candyhouse.app.base

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.R
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.tabs.devices.model.CHLoginViewModel
import co.candyhouse.server.CHLoginAPIManager
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.utils.L
import co.receiver.widget.SesameForegroundService
import co.utils.SharedPreferencesUtils
import co.utils.applyInsetsPadding
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.UserState
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.EasyPermissions

open class BaseActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    val loginViewModel by lazy { ViewModelProvider(this)[CHLoginViewModel::class.java] }
    val deviceViewModel by lazy { ViewModelProvider(this)[CHDeviceViewModel::class.java] }

    private val restartValue = 100
    private val outStateSaveKey = "outStateSaveKey"

    protected lateinit var navController: NavController

    // 底部导航对应的Fragment ID
    private val bottomNavFragmentIds = listOf(
        R.id.deviceListPG,
        R.id.FriendListFG,
        R.id.register
    )

    @SuppressLint("ImplicitSamInstance")
    private fun restartApp(context: Context) {
        stopService(Intent(this, SesameForegroundService::class.java))
        finishAndRemoveTask()
        SystemClock.sleep(30)
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(outStateSaveKey, restartValue)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        L.d("sf", "BaseActivity onCreate...")
        L.d(
            "sf",
            "***version info***: ${BuildConfig.BUILD_TYPE}:${BuildConfig.VERSION_NAME}:${BuildConfig.GIT_HASH}"
        )
        super.onCreate(savedInstanceState)
        val isRestart = savedInstanceState?.getInt(outStateSaveKey, 0) ?: 0
        if (isRestart == restartValue) {
            restartApp(this)
            return
        }

        setContentView(R.layout.activity_main)

        // 解决Android 16以上边到边问题，增加顶部距离
        findViewById<View>(R.id.main_container)?.let {
            applyInsetsPadding(it, top = true)
        }

        // 初始化视图
        initializeMainView(findViewById(R.id.bottom_nav))

        getPermissions()
    }

    private fun initializeMainView(bottomNav: BottomNavigationView) {
        // 获取NavHostFragment和NavController
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_container) as NavHostFragment
        navController = navHostFragment.navController

        bottomNav.setupWithNavController(navController)

        // 监听导航变化
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                in bottomNavFragmentIds -> {
                    //隐藏导航栏
                    bottomNav.visibility = View.VISIBLE
                }

                else -> bottomNav.visibility = View.GONE
            }
        }

        // 处理底部导航栏 - 使用 margin 而不是 padding
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = v.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = systemBars.bottom
            v.layoutParams = params
            insets
        }

        disableTooltip(bottomNav)
    }

    // 屏蔽长按时弹出 tool tips
    @SuppressLint("RestrictedApi")
    fun disableTooltip(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.tooltipText = null
        }
        if (view is MenuView.ItemView) {
            view.setOnLongClickListener { true }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                disableTooltip(view.getChildAt(i))
            }
        }
    }

    protected fun setAWSUserStateListener() {
        AWSMobileClient.getInstance().addUserStateListener { details ->
            L.d("hcia", "💋----> 登入狀態:" + details?.userState?.name)
            when (details?.userState) {
                UserState.SIGNED_IN -> {
                    AWSStatus.setAWSLoginStatus(true)
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val attributes = AWSMobileClient.getInstance().getUserAttributes()
                            AWSStatus.setSubUUID(attributes["sub"])
                        } catch (_: Exception) {
                        }
                    }
                    CHLoginAPIManager.uploadUserDeviceToken {}
                    if (loginViewModel.isJustLogin) {
                        L.d("hcia", "💋 第一次登入上傳所有鑰匙" + loginViewModel.isJustLogin)
                        loginViewModel.isJustLogin = false
                        deviceViewModel.saveKeysToServer()
                    }
                }
                UserState.SIGNED_OUT -> {
                    L.d("hcia", "SIGNED_OUT:")
                    AWSStatus.setAWSLoginStatus(false)
                    AWSStatus.setSubUUID(null)
                    SharedPreferencesUtils.nickname = null
                    SharedPreferencesUtils.userId = null
                }
                else -> {
                    AWSStatus.setAWSLoginStatus(false)
                }
            }
            loginViewModel.gUserState.value = details.userState
        }
    }

    private fun getPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (EasyPermissions.hasPermissions(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            ) {
                CHBleManager.enableScan {}
            } else {
                L.d(
                    "getPermissions",
                    "(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S):true"
                )
                EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.launching_why_need_location_permission),
                    0,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (EasyPermissions.hasPermissions(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            ) {
                CHBleManager.enableScan {}
            } else {
                EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.launching_why_need_location_permission),
                    0,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            }
        } else {
            if (EasyPermissions.hasPermissions(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH
                )
            ) {
                CHBleManager.enableScan {}
            } else {
                EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.launching_why_need_location_permission),
                    0,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            }
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        val filteredPerms = perms.filter { it != Manifest.permission.CAMERA }
        if (filteredPerms.isEmpty()) {
            return
        }

        filteredPerms.forEach {
            if (it != Manifest.permission.POST_NOTIFICATIONS) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.launching_why_need_location_permission),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        CHBleManager.enableScan {}
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }
}

fun BottomNavigationView.setPage(index: Int) {
    selectedItemId = menu[index].itemId
    menu[index].isChecked = true
}