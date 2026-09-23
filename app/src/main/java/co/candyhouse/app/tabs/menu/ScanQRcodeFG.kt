package co.candyhouse.app.tabs.menu

import android.Manifest
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import co.candyhouse.app.R
import co.candyhouse.app.base.setPage
import co.candyhouse.app.databinding.ActivitySimpleScannerBinding
import co.candyhouse.app.ext.aws.AWSStatus
import co.candyhouse.app.ext.webview.manager.WebViewPoolManager
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.util.alertview.fragments.toastMSG
import co.candyhouse.app.util.base64decodeHex
import co.candyhouse.app.util.hexStringToByteArray
import co.candyhouse.app.util.qrcode.core.QRCodeView
import co.candyhouse.sesame.BaseFG
import co.candyhouse.sesame.open.devices.base.CHProductModel
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.utils.L
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.EasyPermissions
import java.io.FileNotFoundException

class ScanQRcodeFG : BaseFG<ActivitySimpleScannerBinding>(), QRCodeView.Delegate,
    EasyPermissions.PermissionCallbacks {

    private val mDeviceModel: CHDeviceViewModel by activityViewModels()
    override fun getViewBinder() = ActivitySimpleScannerBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getPermissions()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.backZone.setOnClickListener {
            findNavController().navigateUp()
        }

        bind.loadImg.setOnClickListener {
            try {
                startActivityForResult(
                    Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    ), 111
                )
            } catch (exp: Exception) {
                L.d("hcia", "exp:$exp")
            }
        }

        bind.imgRestart.setOnClickListener {
            startScan()
        }
    }

    override fun onResume() {
        super.onResume()
        if (EasyPermissions.hasPermissions(requireContext(), Manifest.permission.CAMERA)) {
            startScan()
        }
    }

    override fun onPause() {
        super.onPause()
        safeDestroy()
    }

    private fun getPermissions() {
        if (!EasyPermissions.hasPermissions(requireContext(), Manifest.permission.CAMERA)) {
            EasyPermissions.requestPermissions(this, "CAMERA", 0, Manifest.permission.CAMERA)
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(
            context,
            "Please grant camera permission to use the QR Scanner",
            Toast.LENGTH_SHORT
        ).show()
        findNavController().navigateUp()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        L.d("hcia", "onPermissionsGranted requestCode:$requestCode")

        startScan()
    }

    private fun startScan() {
        safeDestroy()
        val binding = bindingOrNull() ?: return
        binding.zxingview.visibility = View.VISIBLE
        binding.imgRestart.visibility = View.GONE
        binding.zxingview.startCamera()
        binding.zxingview.startSpotAndShowRect()
        binding.zxingview.setDelegate(this)
    }

    override fun onScanQRCodeSuccess(result: String?) {
        L.d("parceUrlQr", "qrResult:$result")
        view?.post { bind.proBar.visibility = View.VISIBLE }
        lifecycleScope.launch(Dispatchers.IO) {
            result?.let {
                parceURI(it)
            }
        }
    }

    private fun hideProBar() {
        view?.post {
            bind.proBar.visibility = View.GONE
        }
    }

    private fun parceURI(result: String) {
        try {
            val receiveUri = result.replace("+", "%2B").toUri()

            if (!receiveUri.isHierarchical) return

            val type = receiveUri.getQueryParameter("t")

            when (type) {
                "friend" -> handleFriendType(receiveUri)
                "sk" -> handleSkType(result)
                else -> {
                    qrCodeError(getString(R.string.qrcodeNotSupport))
                }
            }
        } catch (e: Exception) {
            qrCodeError(getString(R.string.qrcodeNotSupport))
            e.printStackTrace()
        }
    }

    private fun qrCodeError(msg: String) {
        view?.post {
            bind.imgRestart.visibility = View.VISIBLE

            hideProBar()
            toastMSG(msg)
        }
    }

    private fun handleFriendType(receiveUri: Uri) {
        val friendID = receiveUri.getQueryParameter("friend")
        friendID?.let {
            CHAPIClientBiz.addFriend(it) {
                it.onSuccess {
                    view?.post {
                        WebViewPoolManager.setPendingRefresh("contacts")
                        findNavController().navigateUp()
                        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
                            ?.setPage(2)
                    }
                }
                it.onFailure {
                    handleFriendFailure()
                }
            }
        }
    }

    private fun handleFriendFailure() {
        if (!AWSStatus.isSignedIn()) {
            toastMSG(getString(R.string.loginNeed))
        }
        activity?.runOnUiThread {
            findNavController().navigateUp()
        }
    }

    // 分享钥匙(sk)：向服务端兑换扫码全文(qrToken)，钥匙归属由服务端完成，客户端只需重拉服务端列表
    private fun handleSkType(qrToken: String) {
        CHAPIClientBiz.redeemQR(qrToken) { result ->
            result.onSuccess { state ->
                val receiveUri = state.data.replace("+", "%2B").toUri()
                val devModel = receiveUri.getQueryParameter("sk")
                    ?.let { runCatching { it.base64decodeHex().hexStringToByteArray() }.getOrNull() }
                    ?.firstOrNull()
                    ?.let { CHProductModel.getByValue(it.toInt()) }
                L.d("handleSkType", "devModel:$devModel")
                if (devModel == null) {
                    qrCodeError(getString(R.string.qrcodeNotSupport))
                    return@onSuccess
                }
                mDeviceModel.refreshDevices()
                view?.post {
                    if (isAdded && !isDetached) {
                        findNavController().navigateUp()
                        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
                            ?.setPage(if (devModel == CHProductModel.SSMFace3) 1 else 0)
                    }
                }
            }
            result.onFailure { error ->
                qrCodeError(error.message ?: getString(R.string.qrcodeNotSupport))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        safeDestroy()
    }

    private fun safeDestroy() {
        val binding = bindingOrNull() ?: return
        try {
            binding.zxingview.onDestroy()
        } catch (e: Exception) {
            // 处理异常
            e.printStackTrace()
        }
    }

    override fun onCameraAmbientBrightnessChanged(isDark: Boolean) {
    }

    override fun onScanQRCodeOpenCameraError() {
        qrCodeError(getString(R.string.cameraError))

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 111) {
            handleImageResult(data)
        }
    }

    private fun handleImageResult(data: Intent?) {
        data?.data?.let { uri ->
            try {
                val inputStream = requireActivity().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                bitmap?.let {
                    val width = it.width
                    val height = it.height
                    val pixels = IntArray(width * height)
                    it.getPixels(pixels, 0, width, 0, 0, width, height)
                    it.recycle()
                    val source = RGBLuminanceSource(width, height, pixels)
                    val bBitmap = BinaryBitmap(HybridBinarizer(source))
                    val reader = MultiFormatReader()
                    try {
                        val result = reader.decode(bBitmap)
                        L.d("hcia", "result:" + result.text)
                        parceURI(result.text)
                    } catch (e: NotFoundException) {
                        L.d("hcia", "decode exception$e")
                    }
                }
            } catch (e: FileNotFoundException) {
                L.d("hcia", "can not open file$uri$e")
            }
        }
    }

}
