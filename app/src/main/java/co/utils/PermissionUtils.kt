package co.utils

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import co.candyhouse.app.CandyHouseApp
import co.candyhouse.sesame.open.CHDeviceManager

/**
 * Created by wuying@cn.candyhouse.co on 19/09/2021.
 * This class is used for check permission
 */
object PermissionUtils {


    const val REQUEST_CODE_FOREGROUND_NOTIFICATION_PERMISSION = 201
    const val REQUEST_CODE_NFC_PERMISSION = 100


    /**
     * 检查单个权限是否已被授予。如果没有，则发起请求
     *
     * @param activity 当前的 Activity。
     * @param permission 要检查的权限。
     * @param requestCode 请求码，用于在回调中识别权限请求。
     * @return 如果权限已被授予，返回 true；否则返回 false，并请求权限。
     */
    fun checkAndRequestPermission(
        activity: Activity?, permission: String, requestCode: Int
    ): Boolean {
        return if (ContextCompat.checkSelfPermission(
                CHDeviceManager.app, permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activity?.let {
                ActivityCompat.requestPermissions(it, arrayOf(permission), requestCode)
            }
            false
        } else {
            true
        }
    }

    /**
     * 检查多个权限是否都已被授予。如果没有，则发起请求
     *
     * @param activity 当前的 Activity。
     * @param permissions 要检查的权限数组。
     * @param requestCode 请求码，用于在回调中识别权限请求。
     * @return 如果所有权限都已被授予，返回 true；否则返回 false，并请求未授予的权限。
     */
    fun checkAndRequestPermissions(
        activity: Activity?, permissions: Array<String>, requestCode: Int
    ): Boolean {
        val deniedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(CHDeviceManager.app, it) != PackageManager.PERMISSION_GRANTED
        }

        return if (deniedPermissions.isNotEmpty()) {
            activity?.let {
                ActivityCompat.requestPermissions(it, deniedPermissions.toTypedArray(), requestCode)
            }
            false
        } else {
            true
        }
    }

    /**
     * 处理权限请求的结果。
     *
     * @param requestCode 请求码，用于识别权限请求。
     * @param permissions 请求的权限数组。
     * @param grantResults 权限请求结果数组。
     * @return 如果所有请求的权限都被授予，返回 true；否则返回 false。
     */
    fun handlePermissionResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ): Boolean {
        val deniedPermissions = permissions.filterIndexed { index, _ ->
            grantResults[index] != PackageManager.PERMISSION_GRANTED
        }

        return deniedPermissions.isEmpty()
    }
}
