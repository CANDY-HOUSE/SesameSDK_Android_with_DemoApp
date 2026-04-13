package co.candyhouse.app.base

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.viewbinding.ViewBinding
import co.candyhouse.app.R
import co.candyhouse.app.ext.DfuCenter
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.utils.applyBottomInsets
import com.google.android.material.snackbar.Snackbar

abstract class BaseDeviceFG<T : ViewBinding> : BaseNFG<T>(), DfuCenter.Delegate {

    private var dfuText: TextView? = null
    private var snackbar: Snackbar? = null
    private var cachedPageDeviceKey: String? = null

    var isUpload = false
    val mDeviceModel: CHDeviceViewModel by activityViewModels()

    protected open fun providePageDeviceKey(): String? {
        return arguments?.getString("deviceId")
            ?: mDeviceModel.ssmLockLiveData.value?.deviceId?.toString()
    }

    protected fun getPageDeviceKey(): String? {
        if (cachedPageDeviceKey == null) {
            cachedPageDeviceKey = providePageDeviceKey()
        }
        return cachedPageDeviceKey
    }

    override fun onResume() {
        super.onResume()
        getPageDeviceKey()?.let { DfuCenter.attachDelegate(it, this) }
    }

    override fun onPause() {
        super.onPause()
        getPageDeviceKey()?.let { DfuCenter.detachDelegate(it) }
        snackbar?.dismiss()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dfuText = view.findViewById(R.id.device_version_txt)
        snackbar = Snackbar.make(view, "", Snackbar.LENGTH_INDEFINITE)
        applyBottomInsets()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dfuText = null
        snackbar = null
    }

    override fun onDfuState(resId: Int) {
        val textView = dfuText
        if (textView != null) {
            textView.post {
                textView.text = CHDeviceManager.app.getString(resId)
            }
        } else {
            snackbar?.setText(resId)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onDfuProgress(percent: Int) {
        val textView = dfuText
        if (textView != null) {
            textView.post {
                textView.text = "$percent%"
            }
        } else {
            snackbar?.setText("$percent%")
        }
    }

    override fun onDfuError(message: String?) {
        val msg = CHDeviceManager.app.getString(R.string.dfu_status_error_msg) + ":" + message
        val textView = dfuText
        if (textView != null) {
            textView.post {
                textView.text = msg
            }
        } else {
            snackbar?.apply {
                setAction(R.string.cancel) { dismiss() }
                setText(msg)
            }
        }
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