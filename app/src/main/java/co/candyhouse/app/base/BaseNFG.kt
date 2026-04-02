package co.candyhouse.app.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.sesame.BaseFG
import co.candyhouse.sesame.utils.L

abstract class BaseNFG<T : ViewBinding> : BaseFG<T>() {

    val mDeviceViewModel: CHDeviceViewModel by activityViewModels()

    fun getDeviceNameById(deviceId: String): String? {
        return if (isAdded) {
            mDeviceViewModel.myChDevices.value.firstOrNull { it.deviceId.toString() == deviceId }
                ?.getNickname()
        } else {
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // back_zone: 返回紐
        view.findViewById<View>(R.id.back_zone)?.setOnClickListener {
            // Nav返回前一層
            findNavController().navigateUp()
        }
    }

    override fun onResume() {
        super.onResume()

        L.d("baseFragment", this::class.java.name)
    }

}