package co.candyhouse.app.base.scan

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import cn.bingoogolapple.qrcode.core.QRCodeView
import cn.bingoogolapple.qrcode.zxing.ZXingView
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.app.tabs.friends.FriendsFG
import co.candyhouse.sesame.server.CHAccountManager
//import co.candyhouse.sesame.server.CHQrevent
import co.candyhouse.app.R
import co.candyhouse.sesame.ble.CHBleManager
import co.candyhouse.sesame.ble.CHDeviceManager
import co.utils.L
import pub.devrel.easypermissions.EasyPermissions


interface ScanCallBack {
    fun onScanFriendSuccess(friendID: String)
}

class ScanFG : Fragment(), QRCodeView.Delegate, EasyPermissions.PermissionCallbacks {
    companion object {
        var callBack: ScanCallBack? = null
    }

    private var originalColor: Int? = null
    lateinit var mZXingView: ZXingView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getPermissions()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_simple_scanner, container, false)

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val backIcon = view.findViewById(R.id.backicon) as View
        mZXingView = view.findViewById(R.id.zxingview)
        backIcon.setOnClickListener {
            findNavController().navigateUp()
        }
        mZXingView.setDelegate(this)
    }


    override fun onResume() {
        super.onResume()
        originalColor = MainActivity.activity?.getWindow()?.statusBarColor
        MainActivity.activity?.getWindow()?.statusBarColor = resources.getColor(R.color.black)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity!!.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
        (activity as MainActivity).hideMenu()

        if (EasyPermissions.hasPermissions(context!!, Manifest.permission.CAMERA)) {
            mZXingView.startCamera()
            mZXingView.startSpotAndShowRect()
//            mZXingView.postDelayed({
//                mZXingView.visibility = View.VISIBLE
//            },1000)
        }
    }

    override fun onPause() {
        super.onPause()

        MainActivity.activity?.getWindow()?.statusBarColor = originalColor!!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity!!.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
//            window.statusBarColor = Color.WHITE
        }
        mZXingView.stopCamera()
    }

    override fun onDestroy() {
        mZXingView.onDestroy()
        super.onDestroy()
    }


    private fun getPermissions() {

        if (EasyPermissions.hasPermissions(context!!, Manifest.permission.CAMERA)) {
        } else {
            EasyPermissions.requestPermissions(
                    this, "CAMERA", 0,
                    Manifest.permission.CAMERA
            )
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(context, "Please grant camera permission to use the QR Scanner", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    override fun onScanQRCodeSuccess(result: String?) {

        L.d("hcia", "qrcode:" + result)
        val receiveUri = Uri.parse(result?.replace("+", "%2B"))
        L.d("hcia", "receiveUri scheme:" + receiveUri.scheme)//candyhouse
        L.d("hcia", "receiveUri authority:" + receiveUri.authority)//SesameUI
        L.d("hcia", "receiveUri t:" + receiveUri.getQueryParameter("t"))//[]
        L.d("hcia", "receiveUri t:" + receiveUri.getQueryParameter("sharedKey"))//[]
        val sharedKey = "sharedKey"
        val sharek = receiveUri.getQueryParameter(sharedKey)
        L.d("hcia", "sharedKey:" + sharedKey)
        L.d("hcia", "sharek:" + sharek)

        CHDeviceManager.receiveSesame2Keys(sharek!!) {
            it.onSuccess {
                it.data.forEach {
                    L.d("hcia", "設定歷史標籤 deviceId:" + it.deviceId)
                    it.setHistoryTag("のび太".toByteArray()) {
                    }
                }
            }
            it.onFailure {
                L.d("hcia", "it:" + it)
            }
        }


        findNavController().navigateUp()
    }

    override fun onCameraAmbientBrightnessChanged(isDark: Boolean) {
    }

    override fun onScanQRCodeOpenCameraError() {
    }
}
