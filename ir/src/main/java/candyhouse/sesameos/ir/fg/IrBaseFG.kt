package candyhouse.sesameos.ir.fg


import android.os.Bundle
import android.os.Looper
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import candyhouse.sesameos.ir.R
import co.candyhouse.sesame.BaseFG


import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType


abstract class IrBaseFG<T : ViewBinding>() : BaseFG<T>() {
    fun showCustomDialog(dialogTitle: String, editText: String = "", tips:String = "",callOK: (String) -> Unit = {}) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())

        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.ir_dialog_custom, null)

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)




        setStatusColor(R.color.gray0)

        view.findViewById<ImageView>(R.id.btnClose)?.setOnClickListener {
            findNavController().popBackStack()
        }

    }

    fun hasAction(actionId: Int): Boolean {


        val action = this.findNavController().currentDestination?.getAction(actionId)
        return action != null || this.findNavController().graph.getAction(actionId) != null
    }

    fun isExst(block: () -> Unit = {}) {
        if (isAdded && !isDetached) {
            if (Looper.getMainLooper() != Looper.myLooper()) {
                requireActivity().runOnUiThread {
                    block()
                }
            } else {
                block()
            }


        }
    }

    fun Fragment.safeNavigate(actionId: Int) {
        if (!isAdded) return
        this.safeNavigate(actionId, null)
    }

    fun safeNavigateBack() {
        if (!isAdded) return

        findNavController().navigateUp()
    }

    fun Fragment.safeNavigate(actionId: Int, bundle: Bundle?) {
        try {
            if (!isAdded) return
            val navController = findNavController()
            if (hasAction(actionId)) {
                navController.navigate(actionId, bundle)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setStatusColor() {
        setStatusColor(R.color.white)
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

    fun imgTagSet() {
        view?.findViewById<ImageView>(R.id.imgRight)?.visibility = View.VISIBLE

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

    private fun initBind() {
        val type = this.javaClass.genericSuperclass
        if (type is ParameterizedType) {
            val tClass: Class<T> = (type as ParameterizedType).actualTypeArguments[0] as Class<T>
            try {
                val method = tClass.getMethod("inflate", LayoutInflater::class.java)
                bind = method.invoke(null, layoutInflater) as T
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
    }


    private val cameraPermissionRequestCode = 100


}