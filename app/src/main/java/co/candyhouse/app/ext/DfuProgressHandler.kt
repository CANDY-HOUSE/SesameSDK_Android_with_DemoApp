package co.candyhouse.app.ext

import android.annotation.SuppressLint
import android.widget.TextView
import co.candyhouse.app.R
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.utils.L
import com.google.android.material.snackbar.Snackbar
import no.nordicsemi.android.dfu.DfuProgressListener

/**
 * 进度监听
 *
 * @author frey on 2025/4/8
 */
class DfuProgressHandler(
    private val dfuText: TextView?,
    private val snackbar: Snackbar?
) : DfuProgressListener {

    override fun onDeviceConnecting(deviceAddress: String) {
        handleSetText(R.string.onDeviceConnecting)
    }

    override fun onDeviceConnected(deviceAddress: String) {
        handleSetText(R.string.onDeviceConnected)
    }

    override fun onDfuProcessStarting(deviceAddress: String) {
        handleSetText(R.string.onDfuProcessStarting)
    }

    override fun onDfuProcessStarted(deviceAddress: String) {
        handleSetText(R.string.onDfuProcessStarted)
    }

    override fun onEnablingDfuMode(deviceAddress: String) {
        handleSetText(R.string.onEnablingDfuMode)
    }

    @SuppressLint("SetTextI18n")
    override fun onProgressChanged(
        deviceAddress: String,
        percent: Int,
        speed: Float,
        avgSpeed: Float,
        currentPart: Int,
        partsTotal: Int
    ) {
        dfuText?.post {
            dfuText.text = "$percent%"
        } ?: snackbar?.setText("$percent%")
    }

    override fun onFirmwareValidating(deviceAddress: String) {
        handleSetText(R.string.onFirmwareValidating)
    }

    override fun onDeviceDisconnecting(deviceAddress: String?) {
        handleSetText(R.string.onDeviceDisconnecting)
    }

    override fun onDeviceDisconnected(deviceAddress: String) {
        handleSetText(R.string.onDeviceDisconnected)
    }

    override fun onDfuCompleted(deviceAddress: String) {
        handleSetText(R.string.onDfuCompleted)
        snackbar?.dismiss()
    }

    override fun onDfuAborted(deviceAddress: String) {
        handleSetText(R.string.onDfuAborted)
    }

    @SuppressLint("SetTextI18n")
    override fun onError(deviceAddress: String, error: Int, errorType: Int, message: String?) {
        dfuText?.post {
            dfuText.text =
                CHDeviceManager.app.getString(R.string.dfu_status_error_msg) + ":" + message
        } ?: snackbar?.apply {
            setAction(R.string.cancel) {
                snackbar.dismiss()
            }
            setText(CHDeviceManager.app.getString(R.string.dfu_status_error_msg) + ":" + message)
        }
    }

    // 统一处理文本显示
    private fun handleSetText(resId: Int) {
        dfuText?.post {
            dfuText.text = CHDeviceManager.app.getString(resId)
        } ?: snackbar?.setText(resId)
    }

}