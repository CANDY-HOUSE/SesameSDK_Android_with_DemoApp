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

package co.candyhouse.app.tabs.devices.ssm2.setting

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import cn.bingoogolapple.qrcode.core.BGQRCodeUtil
import cn.bingoogolapple.qrcode.zxing.QRCodeEncoder
import co.candyhouse.app.base.BaseNFG
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.sesame.server.CHAccountManager
import co.candyhouse.app.R
import co.utils.textdrawable.util.avatatImagGenaroter
import kotlinx.android.synthetic.main.back_sub.*
import kotlinx.android.synthetic.main.fg_my_ssmkey.*


class MyKEYFG : BaseNFG() {
    private var originalColor: Int? = null

    companion object {
        var ssm2key: String? = null
        var keyname: String? = null
    }

    override fun onPause() {
        super.onPause()
        MainActivity.activity?.getWindow()?.statusBarColor = originalColor!!
    }

    override fun onResume() {
        super.onResume()
        originalColor = MainActivity.activity?.getWindow()?.statusBarColor
        MainActivity.activity?.getWindow()?.statusBarColor = resources.getColor(R.color.gray0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fg_my_ssmkey, container, false)
        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val keyURI = Uri.Builder()
        keyURI.scheme("candyhouse")
        keyURI.authority("SesameUI")
        keyURI.appendQueryParameter("t", "sharedKey")
        keyURI.appendQueryParameter("sharedKey", ssm2key)
        val uri = keyURI.build()
        val image = QRCodeEncoder.syncEncodeQRCode(uri.toString(), BGQRCodeUtil.dp2px(context, 150f))
        qrcode.setImageBitmap(image)
        headv.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        headv?.setImageDrawable(avatatImagGenaroter(keyname))
        backicon.setOnClickListener {
            findNavController().navigateUp()
        }
    }
}
