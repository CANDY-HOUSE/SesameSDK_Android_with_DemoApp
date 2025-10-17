package co.candyhouse.app.base

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.viewbinding.ViewBinding
import co.candyhouse.app.R
import co.candyhouse.app.ext.DfuProgressHandler
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.sesame.open.device.CHDevices
import co.utils.applyBottomInsets
import com.google.android.material.snackbar.Snackbar
import no.nordicsemi.android.dfu.DfuServiceListenerHelper

abstract class BaseDeviceFG<T : ViewBinding> : BaseNFG<T>() {

    private val dfuText by lazy { view?.findViewById<TextView>(R.id.device_version_txt) }
    private val snackbar by lazy { view?.let { Snackbar.make(it, "", Snackbar.LENGTH_INDEFINITE) } }

    // 使用委托类单独监听事件
    private val dfuProgressListener by lazy {
        DfuProgressHandler(dfuText, snackbar)
    }

    var isUpload = false
    val mDeviceModel: CHDeviceViewModel by activityViewModels()

    override fun onResume() {
        super.onResume()
        DfuServiceListenerHelper.registerProgressListener(requireActivity(), dfuProgressListener)
    }

    override fun onPause() {
        super.onPause()
        DfuServiceListenerHelper.unregisterProgressListener(requireActivity(), dfuProgressListener)
        snackbar?.dismiss()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyBottomInsets()
    }

    fun hasAddedOrIsGuestKey(device: CHDevices): Boolean {
        if (device.getLevel() == 2) {
            return true
        }
        val stringArrayList = arguments?.getStringArrayList("data")
        return stringArrayList?.contains(device.deviceId.toString()) ?: false
    }

    protected fun runOnUiThread(action: Runnable) {
        if (isAdded && !isDetached) {
            activity?.runOnUiThread(action)
        }
    }

}