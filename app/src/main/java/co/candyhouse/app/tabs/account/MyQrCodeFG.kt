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
package co.candyhouse.app.tabs.account

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import cn.bingoogolapple.qrcode.core.BGQRCodeUtil
import cn.bingoogolapple.qrcode.zxing.QRCodeEncoder
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.base.BaseNFG
import co.candyhouse.app.databinding.FgMyqrBinding
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.utils.L
import co.utils.SharedPreferencesUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MyQrCodeFG : BaseNFG<FgMyqrBinding>() {
    override fun getViewBinder() = FgMyqrBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.shareZone.setOnClickListener {
            view.isDrawingCacheEnabled = true
            view.buildDrawingCache(true)
            val bitmap = view.drawingCache
            shareImage(bitmap)
            view.destroyDrawingCache()
            L.d("hcia", "share_zone:")
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val subUUID = SharedPreferencesUtils.userId
                L.d("hcia", "subUUID:$subUUID")
                val userURI = Uri.Builder()
                userURI.scheme("ssm")
                userURI.authority("UI")
                userURI.appendQueryParameter("t", "friend")
                userURI.appendQueryParameter("friend", subUUID)
                val uri = userURI.build()
                val image = QRCodeEncoder.syncEncodeQRCode(uri.toString(), BGQRCodeUtil.dp2px(CHDeviceManager.app, 150f))

                withContext(Dispatchers.Main) {
                    bind.customName.text = SharedPreferencesUtils.nickname
                    bind.qrcode.setImageBitmap(image)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun shareImage(bitmap: Bitmap) {
        try {
            val dir = File(requireActivity().getExternalFilesDir("candy"), "key")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            L.d("hcia", "dir:$dir")
            val img = File(dir, "me" + ".png")
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
            share.putExtra(Intent.EXTRA_TEXT, "candy house")
            share.putExtra(Intent.EXTRA_STREAM, uri)
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            startActivity(Intent.createChooser(share, "Share via"))
        } catch (e: Exception) {
            L.d("hcia", "e:$e")
            Toast.makeText(activity, "Something Went Wrong, Please Try Again!", Toast.LENGTH_SHORT).show()
        }
    }

}