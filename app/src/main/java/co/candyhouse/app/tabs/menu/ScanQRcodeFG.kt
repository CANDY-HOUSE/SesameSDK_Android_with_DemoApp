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
import cn.bingoogolapple.qrcode.core.QRCodeView
import co.candyhouse.app.R
import co.candyhouse.app.base.setPage
import co.candyhouse.app.databinding.ActivitySimpleScannerBinding
import co.candyhouse.app.tabs.account.cheyKeyToUserKey
import co.candyhouse.app.tabs.account.getHistoryTag
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.tabs.devices.model.CHUserViewModel
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.app.tabs.devices.ssm2.setLevel
import co.candyhouse.app.tabs.devices.ssm2.setNickname
import co.candyhouse.server.CHLoginAPIManager
import co.candyhouse.sesame.BaseFG
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.utils.L
import co.utils.alertview.fragments.toastMSG
import co.utils.base64decodeHex
import co.utils.hexStringToByteArray
import co.utils.noHashtoUUID
import co.utils.toHexString
import com.amazonaws.mobile.client.AWSMobileClient
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

    private val mFriendModel: CHUserViewModel by activityViewModels()
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
            bind.zxingview.visibility = View.VISIBLE
            bind.zxingview.startCamera()
            bind.zxingview.startSpotAndShowRect()
            bind.zxingview.setDelegate(this)
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
        bind.zxingview.visibility = View.VISIBLE

        startScan()
    }

    private fun startScan() {
        safeDestroy()
        bind.imgRestart.visibility = View.GONE
        bind.zxingview.startCamera()
        bind.zxingview.startSpotAndShowRect()
        bind.zxingview.setDelegate(this)
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
                "sk" -> handleSkType(receiveUri)
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
            CHLoginAPIManager.addFriend(it) {
                it.onSuccess {
                    mFriendModel.myFriends.value.clear()
                    mFriendModel.syncFriendsFromServer()
                    view?.post {
                        findNavController().navigateUp()
                        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
                            ?.setPage(1)
                    }
                }
                it.onFailure {
                    handleFriendFailure()
                }
            }
        }
    }

    private fun handleFriendFailure() {
        if (!AWSMobileClient.getInstance().isSignedIn) {
            toastMSG(getString(R.string.loginNeed))
        } else {
            mFriendModel.myFriends.value.clear()
            mFriendModel.syncFriendsFromServer()
        }
        activity?.runOnUiThread {
            findNavController().navigateUp()
        }
    }

    private fun handleSkType(receiveUri: Uri) {
        val sharedKey = receiveUri.getQueryParameter("sk")
        val level = receiveUri.getQueryParameter("l")
        val customName = receiveUri.getQueryParameter("n")
        val parameterNames = receiveUri.queryParameterNames
        for (name in parameterNames) {
            val value = receiveUri.getQueryParameter(name)
            L.d("handleSkType", "Parameter:name:$name----$value")
        }

        sharedKey?.let {
            val keyData = it.base64decodeHex().hexStringToByteArray()
            val devModel = CHProductModel.getByValue(keyData[0].toInt())
            L.d("handleSkType", "devModel:$devModel")
            if (devModel.isValidModel()) {
                handleValidModel(keyData, level, customName)
            } else {
                handleInvalidModel(keyData, level, customName)
            }
        }
    }

    private fun CHProductModel?.isValidModel(): Boolean {
        return this == CHProductModel.SS5 || this == CHProductModel.BiKeLock2 || this == CHProductModel.SSMTouchPro ||
                this == CHProductModel.SSMTouch || this == CHProductModel.SS5PRO || this == CHProductModel.BLEConnector ||
                this == CHProductModel.Remote || this == CHProductModel.SS5US || this == CHProductModel.SesameBot2 ||
                this == CHProductModel.SSMFacePro || this == CHProductModel.SSMFaceProAI || this == CHProductModel.SSMFaceAI || this == CHProductModel.SS6Pro ||
                this == CHProductModel.Hub3 || this == CHProductModel.SSMFace || this == CHProductModel.SSMOpenSensor2
    }

    override fun onDestroy() {
        super.onDestroy()
        safeDestroy()
    }

    private fun safeDestroy() {
        bind?.let { binding ->
            try {
                binding.zxingview.onDestroy()
            } catch (e: Exception) {
                // 处理异常
                e.printStackTrace()
            }
        }
    }

    private fun handleValidModel(keyData: ByteArray, level: String?, customName: String?) {
        val modelStr = CHProductModel.getByValue(keyData[0].toInt())?.deviceModel()!!
        val secretHex = keyData.sliceArray(1..16).toHexString()
        val pubHex = keyData.sliceArray(17..20).toHexString()
        val keyIndexHex = keyData.sliceArray(21..22).toHexString()
        val uuidHex = keyData.sliceArray(23..38).toHexString()
        val uuidStr = uuidHex.noHashtoUUID().toString().lowercase()

        val receiveDevoiceKey =
            CHDevice(uuidStr, modelStr, getHistoryTag(), keyIndexHex, secretHex, pubHex)
        CHDeviceManager.receiveCHDeviceKeys(receiveDevoiceKey) {
            it.onSuccess {
                it.data.forEach { device ->
                    device.setLevel(level!!.toInt())
                    device.setNickname(customName!!)
                    if (device.getLevel() == -1) {
                        return@forEach
                    }
                    CHLoginAPIManager.putKey(
                        cheyKeyToUserKey(
                            device.getKey(),
                            device.getLevel(),
                            device.getNickname()
                        )
                    ) {}
                    mDeviceModel.updateDevices()
                    view?.post {
                        if (isAdded && !isDetached) {
                            findNavController().navigateUp()
                            requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
                                ?.setPage(0)
                        }
                    }
                }
            }
            it.onFailure {
                qrCodeError(getString(R.string.addFail))
            }
        }
    }

    private fun handleInvalidModel(keyData: ByteArray, level: String?, customName: String?) {

        L.d("handleInvalidModel", "l" + keyData.size)
        val modelStr = CHProductModel.getByValue(keyData[0].toInt())?.deviceModel()!!
        val secretHex = keyData.sliceArray(1..16).toHexString()
        val pubHex = keyData.sliceArray(17..80).toHexString()
        val keyIndexHex = keyData.sliceArray(81..82).toHexString()
        val uuidHex = keyData.sliceArray(83..98).toHexString()
        val uuidStr = uuidHex.noHashtoUUID().toString().lowercase()

        val receiveDevoiceKey =
            CHDevice(uuidStr, modelStr, getHistoryTag(), keyIndexHex, secretHex, pubHex)
        CHDeviceManager.receiveCHDeviceKeys(receiveDevoiceKey) {
            it.onSuccess {
                it.data.forEach { device ->
                    device.setLevel(level?.toInt()!!)
                    device.setNickname(customName!!)
                    if (device.getLevel() == -1) {
                        return@forEach
                    }
                    CHLoginAPIManager.putKey(
                        cheyKeyToUserKey(
                            device.getKey(),
                            device.getLevel(),
                            device.getNickname()
                        )
                    ) {}
                    mDeviceModel.updateDevices()
                    view?.post {
                        findNavController().navigateUp()
                        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
                            ?.setPage(0)
                    }
                }
            }
            it.onFailure {
                qrCodeError(getString(R.string.addFail))
            }
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
