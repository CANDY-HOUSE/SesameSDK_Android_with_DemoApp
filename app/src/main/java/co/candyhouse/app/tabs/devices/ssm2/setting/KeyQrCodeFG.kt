package co.candyhouse.app.tabs.devices.ssm2.setting

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider

import co.candyhouse.app.BuildConfig
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.app.tabs.devices.ssm2.modelName
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.open.device.CHDevices
import co.utils.L
import co.utils.base64Encode
import co.utils.hexStringToByteArray
import co.utils.toHexString
import kotlinx.android.synthetic.main.fg_my_ssmkey.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class KeyQrCodeFG : BaseDeviceFG(R.layout.fg_my_ssmkey) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val targetDevice: CHDevices = mDeviceModel.ssmLockLiveData.value!!
        key_id.text = targetDevice.getNickname()

        sub_title.text = getString(R.string.ssm_qr_hint,targetDevice.productModel.modelName())
        key_id.text = targetDevice.getNickname()

        if (targetDevice.getLevel() == -1) {
            L.d("hcia", "something wrong device qrcdoe generate!")
            return
        }
        if (mDeviceModel.guestKeyId != null) {//recover guestKey
            setKey(targetDevice, targetDevice.getKey().copy(secretKey = mDeviceModel.guestKeyId!!))
            mDeviceModel.guestKeyId = null
        } else if (mDeviceModel.targetShareLevel == 2 && targetDevice.getLevel() != 2) {//guestKey
            targetDevice.createGuestKey(SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date())) {
                it.onSuccess {
                    MainActivity.activity!!.runOnUiThread {
                        setKey(targetDevice, it.data)
                    }
                }
            }
        } else {
            setKey(targetDevice, targetDevice.getKey())
        }
    }

    private fun setKey(targetDevice: CHDevices, candyDevice: CHDevice) {
        L.d("hcia", "getNickname():" + targetDevice.getNickname())
        L.d("hcia", "candyDevice:" + candyDevice)
        L.d("hcia", "productType:" + byteArrayOf(targetDevice.productModel.productType().toByte()).toHexString())
        L.d("hcia", "secretKey:" + candyDevice.secretKey)
        L.d("hcia", "sesame2PublicKey:" + candyDevice.sesame2PublicKey)
        L.d("hcia", "keyIndex:" + candyDevice.keyIndex)
        L.d("hcia", "andyDevice.deviceUUID:" + candyDevice.deviceUUID.uppercase())
        L.d("hcia", "nohashdeviceUUID:" + candyDevice.deviceUUID.replace("-", ""))
        val keydata = byteArrayOf(targetDevice.productModel.productType().toByte()).toHexString() + candyDevice.secretKey + candyDevice.sesame2PublicKey + candyDevice.keyIndex + candyDevice.deviceUUID.replace("-", "")
        L.d("hcia", "keydata:" + keydata)
        val littleKey = keydata.hexStringToByteArray().base64Encode()
        val keyURI = Uri.Builder().apply {
            scheme("ssm")
            authority("UI")
            appendQueryParameter("t", "sk")
            appendQueryParameter("sk", littleKey)
            appendQueryParameter("l", mDeviceModel.targetShareLevel.toString())
            appendQueryParameter("n", targetDevice.getNickname())
        }.build()
        context?.let {
            val mIcon = BitmapFactory.decodeResource(it.resources, R.mipmap.ic_launcher_round)
//        L.d("hcia", "🧵 keyURI:" + keyURI)

        }

    }

    private fun shareImage(bitmap: Bitmap, targetDevice: CHDevices) {

        try {

            val dir = File(requireContext().getExternalFilesDir("candy"), "key")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            L.d("hcia", "dir:" + dir)
            val img = File(dir, targetDevice.getNickname() + ".png")
            if (img.exists()) {
                img.delete()
            }
            L.d("hcia", "img:" + img)
            val outStream: OutputStream = FileOutputStream(img)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
            outStream.flush()
            outStream.close()

            L.d("hcia", "Uri.fromFile(img):" + Uri.fromFile(img))
            val share = Intent(Intent.ACTION_SEND)
            val uri: Uri = FileProvider.getUriForFile(requireActivity(), BuildConfig.APPLICATION_ID, img)
            L.d("hcia", "uri:" + uri)

            share.type = "image/*"
            share.putExtra(Intent.EXTRA_TEXT, "candy house")
            share.putExtra(Intent.EXTRA_STREAM, uri)
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            startActivity(Intent.createChooser(share, "Share via"))


        } catch (e: Exception) {
            L.d("hcia", "e:" + e)
            Toast.makeText(activity, "Something Went Wrong, Please Try Again!2", Toast.LENGTH_SHORT).show()
        }
    }
}
