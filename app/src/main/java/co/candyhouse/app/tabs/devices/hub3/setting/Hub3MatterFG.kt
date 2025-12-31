package co.candyhouse.app.tabs.devices.hub3.setting

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import cn.bingoogolapple.qrcode.core.BGQRCodeUtil
import cn.bingoogolapple.qrcode.zxing.QRCodeEncoder
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgMatterBinding
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.open.device.CHWifiModule2NetWorkStatus
import co.candyhouse.sesame.utils.L
import co.utils.alertview.fragments.toastMSG

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class Hub3MatterFG : BaseDeviceFG<FgMatterBinding>() {
    private var qrCodeString: String = ""
    private val scope = CoroutineScope(Dispatchers.Main) // 创建一个 CoroutineScope
    override fun getViewBinder() = FgMatterBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (mDeviceModel.ssmLockLiveData.value == null) {
            return
        }

        val targetDevice: CHDevices = mDeviceModel.ssmLockLiveData.value!!
        val isWifiConnect = (targetDevice.mechStatus as? CHWifiModule2NetWorkStatus)?.isIOTWork == true
        val isStatus = targetDevice.deviceStatus.value
        isStatus?.apply {
            if (this == CHDeviceLoginStatus.unlogined) {
                toastMSG("4 - Bluetooth is off")
            }
        }

        bind.keyId.setOnClickListener {
            showPopupWindow(it,bind.keyId.text.toString())
        }

        L.d("isCOnesaada", "states:$isWifiConnect" + "--" + targetDevice.deviceStatus)
        var manualCodeString: String = ""

        targetDevice.apply {
            bind.matterPairingZone.setOnClickListener { }
        }

        scope.launch { // 在页面创建时启动一个新的协程
            while (qrCodeString.isEmpty()) {
                delay(300) // 等待300毫秒
            }
            setKey(targetDevice)
        }

        (targetDevice as CHHub3).getMatterPairingCode { it ->
            it.onSuccess {
                qrCodeString = String(it.data.sliceArray(0..21), Charsets.US_ASCII)
                manualCodeString = String(it.data.sliceArray(22 until it.data.size), Charsets.US_ASCII)
                //bind.idTextViewMATTERManualCode.text = manualCodeString
                bind.keyId.post {
                    bind.keyId.visibility=View.VISIBLE
                    bind.keyId.text = manualCodeString
                    bind.progressBar.visibility = View.GONE

                }

                (targetDevice as CHHub3).openMatterPairingWindow { it ->
                    it.onSuccess {
                        bind.idTextViewMATTERManualCode.post {
                            if (it.data.toInt() == 0) {
                                bind.idTextViewMATTERManualCode.visibility = View.VISIBLE
                            } else {
                                bind.idTextViewMATTERManualCode.text = "这台 Hub3 连接了太多的 Matter 网络， 请重置后再试。"
                                bind.idTextViewMATTERManualCode.visibility = View.VISIBLE

                            }
                        }

                    }
                }

                //  bind.idTextViewMATTERManualCode.text = targetDevice.getNickname()
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()  // 页面销毁时取消协程
    }

    private fun setKey(targetDevice: CHDevices) {
        context?.let {
            QRCodeEncoder.syncEncodeQRCode(qrCodeString, BGQRCodeUtil.dp2px(context, 300f), Color.BLACK).apply {
                bind.qrCodeImg.setImageBitmap(this)

                bind.shareZone.setOnClickListener {
                    view?.let {
                        it.isDrawingCacheEnabled = true
                        it.buildDrawingCache(true)
                        val bitmap = it.drawingCache
                        shareImage(bitmap, targetDevice)
                        it.destroyDrawingCache()
                    }
                }
                bind.qrCodeImg.setOnClickListener {
                    view?.let {
                        it.isDrawingCacheEnabled = true
                        it.buildDrawingCache(true)
                        val bitmap = it.drawingCache
                        shareImage(bitmap, targetDevice)
                        it.destroyDrawingCache()
                    }
                }
            }
        }

    }

    private fun shareImage(bitmap: Bitmap, targetDevice: CHDevices) {

        try {

            val dir = File(requireContext().getExternalFilesDir("candy"), "key")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            L.d("hcia", "dir:$dir")
            val img = File(dir, targetDevice.getNickname() + ".png")
            if (img.exists()) {
                img.delete()
            }
            L.d("hcia", "img:$img")
            val outStream: OutputStream = FileOutputStream(img)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
            outStream.flush()
            outStream.close()

            L.d("hcia", "Uri.fromFile(img):" + Uri.fromFile(img))
            val share = Intent(Intent.ACTION_SEND)
            val uri: Uri = FileProvider.getUriForFile(requireActivity(), BuildConfig.APPLICATION_ID, img)
            L.d("hcia", "uri:$uri")

            share.type = "image/*"
            share.putExtra(Intent.EXTRA_TEXT, "CANDY HOUSE Hub3 MATTER Pairing Code")
            share.putExtra(Intent.EXTRA_STREAM, uri)
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            startActivity(Intent.createChooser(share, "Share via"))


        } catch (e: Exception) {
            L.d("hcia", "e:$e")
            Toast.makeText(activity, "Something Went Wrong, Please Try Again!2", Toast.LENGTH_SHORT).show()
        }
    }
}
