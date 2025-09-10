package co.candyhouse.app.tabs.devices.hub3.setting.ir

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.model.CHDeviceViewModel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.sesame.BaseFG
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


abstract class BaseIRFG<T : ViewBinding> : BaseFG<T>() {

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

    fun setRightTextView(name: String, block: () -> Unit = {}) {
        view?.findViewById<TextView>(R.id.tvRight)?.apply {
            if (name.isEmpty()) {
                visibility = View.GONE
                return
            }
            visibility = View.VISIBLE
            text = name
            setOnClickListener { block() }
        }
    }
    fun setTitle(name: String) {
        view?.findViewById<TextView>(R.id.tvTitle)?.text = name
    }

    fun tvTitleOnclick(name: String, block: () -> Unit) {
        view?.findViewById<TextView>(R.id.tvTitle)?.apply {
            text = name
            setOnClickListener { block() }
        }
    }

    fun getTitleName(): String {
        return view?.findViewById<TextView>(R.id.tvTitle)?.text?.toString() ?: ""
    }

    fun showCustomDialog(dialogTitle: String, editText: String = "", tips:String = "",callOK: (String) -> Unit = {}) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())

        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.fg_remote_control_save_dialog, null)

        builder.setView(dialogView)

        val dialog: AlertDialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()

        val title = dialogView.findViewById<TextView>(R.id.dialog_title)
        val edtName = dialogView.findViewById<EditText>(R.id.edtName)
        val tipsTextView = dialogView.findViewById<TextView>(R.id.ir_edit_tips)
        dialogView.findViewById<TextView>(R.id.tvCancel).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<TextView>(R.id.tvOk).setOnClickListener {
            dialog.dismiss()
            val name = edtName.text.toString()
            callOK(name)

        }
        edtName.setText(editText)
        title.text = dialogTitle
        if (tips.isNotEmpty()) {
            tipsTextView.text = tips
        }
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.75).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    protected fun handleActionOnMainThread(action: () -> Unit) {
        if (view == null) {
            return
        }
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            action()
        }
    }



}