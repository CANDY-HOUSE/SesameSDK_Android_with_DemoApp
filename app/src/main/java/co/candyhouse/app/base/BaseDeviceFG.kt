package co.candyhouse.app.base

import android.widget.TextView
import androidx.fragment.app.activityViewModels
import co.candyhouse.app.R
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.utils.SharedPreferencesUtils
import com.google.android.material.snackbar.Snackbar
import no.nordicsemi.android.dfu.DfuProgressListener
import no.nordicsemi.android.dfu.DfuServiceListenerHelper

open class BaseDeviceFG(layout: Int) : BaseNFG(layout) {

    private val dfuText: TextView? by lazy { getView()?.findViewById<TextView>(R.id.device_version_txt) }
    private val snackbar: Snackbar? by lazy {
        Snackbar.make(requireView(), "", Snackbar.LENGTH_INDEFINITE) // 類似Toast
    }
    val mDeviceModel: CHDeviceViewModel by activityViewModels()

    override fun onResume() {
        super.onResume()
        DfuServiceListenerHelper.registerProgressListener(requireActivity(), dfuLs)
    }

    override fun onPause() {
        super.onPause()
        DfuServiceListenerHelper.unregisterProgressListener(requireActivity(), dfuLs)
        snackbar?.dismiss()
    }
    fun getHistoryTag(): ByteArray {
        return SharedPreferencesUtils.nickname?.toByteArray() ?: MainActivity.activity!!.getString(R.string.unLoginHistoryTag).toByteArray()
    }
    val dfuLs = object : DfuProgressListener {
        override fun onProgressChanged(deviceAddress: String, percent
        : Int, speed: Float, avgSpeed: Float, currentPart: Int, partsTotal: Int) {
            dfuText?.post {
                dfuText?.text = "$percent%"
            } ?: snackbar?.setText("$percent%")

        }

        override fun onDeviceDisconnecting(deviceAddress: String?) {
            dfuText?.post {
                dfuText?.text = getString(R.string.onDeviceDisconnecting)//初期化中…
            } ?: snackbar?.setText(R.string.onDeviceDisconnecting)

        }

        override fun onDeviceDisconnected(deviceAddress: String) {

            dfuText?.post {
                dfuText?.text = getString(R.string.onDeviceDisconnected)//初期化中…
            } ?: snackbar?.setText(R.string.onDeviceDisconnected)

        }

        override fun onDeviceConnected(deviceAddress: String) {

            dfuText?.post {
                dfuText?.text = getString(R.string.onDeviceConnected)//初期化中…
            } ?: snackbar?.setText(R.string.onDeviceConnected)

        }

        override fun onDfuProcessStarting(deviceAddress: String) {

            dfuText?.post {
                dfuText?.text = getString(R.string.onDfuProcessStarting)//初期化中…
            }
                    ?: snackbar?.show()
            snackbar?.setText(R.string.onDfuProcessStarting)

        }

        override fun onDfuAborted(deviceAddress: String) {

            dfuText?.post {
                dfuText?.text = getString(R.string.onDfuAborted)//初期化中…
            } ?: snackbar?.setText(R.string.onDfuAborted)

        }

        override fun onEnablingDfuMode(deviceAddress: String) {

            dfuText?.post {
                dfuText?.text = getString(R.string.onEnablingDfuMode)//初期化中…
            } ?: snackbar?.setText(R.string.onEnablingDfuMode)

        }

        override fun onDfuCompleted(deviceAddress: String) {

            dfuText?.post {
                dfuText?.text = getString(R.string.onDfuCompleted)//初期化中…
            } ?: snackbar?.setText(R.string.onDfuCompleted)
            snackbar?.dismiss()


        }

        override fun onFirmwareValidating(deviceAddress: String) {

            dfuText?.post {
                dfuText?.text = getString(R.string.onFirmwareValidating)//初期化中…
            } ?: snackbar?.setText(R.string.onFirmwareValidating)

        }

        override fun onDfuProcessStarted(deviceAddress: String) {

            dfuText?.post {
                dfuText?.text = getString(R.string.onDfuProcessStarted)//初期化中…
            } ?: snackbar?.setText(R.string.onDfuProcessStarted)

        }

        override fun onDeviceConnecting(deviceAddress: String) {

            dfuText?.post {
                dfuText?.text = getString(R.string.onDeviceConnecting)//初期化中…
            } ?: snackbar?.setText(R.string.onDeviceConnecting)

        }

        override fun onError(deviceAddress: String, error
        : Int, errorType: Int, message: String?) {
            dfuText?.post {
                dfuText?.text = getString(R.string.dfu_status_error_msg) + ":" + message
            } ?: snackbar?.apply {
                setAction(R.string.cancel) {
                    snackbar?.dismiss()
                }
                setText(getString(R.string.dfu_status_error_msg) + ":" + message)
            }

        }

    }

}
