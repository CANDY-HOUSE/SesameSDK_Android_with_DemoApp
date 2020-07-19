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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import cn.bingoogolapple.qrcode.core.BGAQRCodeUtil
import cn.bingoogolapple.qrcode.zxing.QRCodeEncoder
import co.candyhouse.app.base.BaseNFG
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.app.tabs.devices.ssm2.room.avatatImagGenaroter
import co.candyhouse.sesame.server.CHAccountManager
import co.candyhouse.app.R
import kotlinx.android.synthetic.main.back_sub.*
import kotlinx.android.synthetic.main.fg_myqr.*


class MyQrcodeFG : BaseNFG() {
    private var originalColor: Int? = null

    companion object {
        var mailStr: String? = null
        var givenName: String? = null
        var familyName: String? = null
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
        val view = inflater.inflate(R.layout.fg_myqr, container, false)
        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        CHAccountManager.getInvitation() {
//            it.onSuccess {
//                val image = QRCodeEncoder.syncEncodeQRCode(it, BGAQRCodeUtil.dp2px(context, 150f))
//                qrcode.post {
//                    qrcode.setImageBitmap(image)
//                    headv.visibility = View.VISIBLE
//                    progressBar.visibility = View.GONE
//
//                }
//            }
//        }
        mail.text = mailStr
        given_name.text = givenName
        family_name.text = familyName
        headv?.setImageDrawable(avatatImagGenaroter(givenName))
        head?.setImageDrawable(avatatImagGenaroter(givenName))

        backicon.setOnClickListener {
            findNavController().navigateUp()
        }

    }


}
