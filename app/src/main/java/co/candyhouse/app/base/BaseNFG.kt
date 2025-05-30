package co.candyhouse.app.base

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

import android.os.Bundle

import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.sesame.BaseFG
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    fun showPopupWindow(anchorView: View, text: String) {
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.pop_layout_copy, null)
        val tvAction = popupView.findViewById<TextView>(R.id.tvAction)
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWidth = popupView.measuredWidth
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val xCenter = (screenWidth - popupWidth) / 2
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val anchorY = location[1]
        val yOff = anchorY - popupView.measuredHeight - 2
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, xCenter, yOff)
        tvAction.setOnClickListener {
            popupWindow.dismiss()
            copyToClipboard(text)
        }

        GlobalScope.launch(Dispatchers.Main) {
            delay(2500) // 2.5 seconds
            if (popupWindow.isShowing) {
                popupWindow.dismiss()
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)
    }

}