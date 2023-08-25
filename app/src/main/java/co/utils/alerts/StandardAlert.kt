package co.utils.alerts

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import co.candyhouse.app.R
import co.candyhouse.app.tabs.MainActivity.Companion.activity
import kotlinx.android.synthetic.main.alert_standard.*
import co.utils.alerts.ext.AlertType
import co.utils.alerts.ext.InputType

class StandardAlert(context: Context, val type: AlertType) : BaseAlert(context) {

    var titleText: String? = null
    var lastName: String? = null
    var firstName: String? = null
    var topHinText: String? = null
    var secondHinText: String? = null
    var inputType = InputType.Text

    private var cancelText: String? = null
    private var confirmText: String? = null

    private var mOnCancel: ((StandardAlert) -> Unit)? = null
    private var mOnConfirm: ((StandardAlert) -> Unit)? = null
    private var mOnConfirmWithText: ((StandardAlert, String) -> Unit)? = null
    private var mOnConfirmWithDoubleEdit: ((StandardAlert, String, String) -> Unit)? = null

    override val layout: Int get() = R.layout.alert_standard

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        update()

        when (type) {
            AlertType.Normal -> btnConfirm.setBackgroundResource(R.drawable.bg_btn_confirm)
            AlertType.Warning -> btnConfirm.setBackgroundResource(R.drawable.bg_btn_warning)
            AlertType.InputText -> {
                etInput.setText(lastName)
                etInput.inputType = inputType.value
                etInput__2.visibility = View.GONE
                hintl2.visibility = View.GONE
                hint_second_tx.visibility = View.GONE
                hint_top.text = topHinText
//                hint_top.visibility = View.GONE
            }
            AlertType.TwoEdit -> {
//                etInput.hint = hintText
                etInput.setText(lastName)
                etInput__2.setText(firstName)

                etInput.inputType = inputType.value
                etInput__2.inputType = inputType.value
                etInput.visibility = View.VISIBLE
                etInput__2.visibility = View.VISIBLE
                hint_top.text = topHinText
                hint_second_tx.text = secondHinText

            }
            else -> {}
        }


    }

    override fun onCreatePanelView(featureId: Int): View? {
        return super.onCreatePanelView(featureId)

    }

    fun update() {

        titleText?.let {
            tvTitle.text = it
            tvTitle.visibility = View.VISIBLE
        }

//        contentText?.let {
//        }

        cancelText?.let {
//            btnCancel.text = it
            btnCancel.visibility = View.VISIBLE
            btnCancel.setOnClickListener {
                mOnCancel?.invoke(this@StandardAlert) ?: this@StandardAlert.dismiss()
            }
        }

        confirmText?.let {
            btnConfirm.text = it
            btnConfirm.visibility = View.VISIBLE
            btnConfirm.setOnClickListener {
//                L.d("hcia", "type:" + type)
                when (type) {
                    AlertType.TwoEdit -> {

                        if (etInput.text.isNullOrBlank() || etInput__2.text.isNullOrBlank()) {
                            val shake = AnimationUtils.loadAnimation(context, R.anim.shake)
                            etInput.startAnimation(shake)
                        } else {
                            mOnConfirmWithDoubleEdit?.invoke(this@StandardAlert, etInput.text.toString(), etInput__2.text.toString())
                        }
                    }
                    else -> {

                        mOnConfirm?.invoke(this@StandardAlert) ?: mOnConfirmWithText?.let {
                            if (etInput.text.isNullOrBlank()) {
                                val shake = AnimationUtils.loadAnimation(context, R.anim.shake)
                                etInput.startAnimation(shake)
                            } else {
                                mOnConfirmWithText?.invoke(this@StandardAlert, etInput.text.toString())
                                mOnConfirmWithText = null
                            }
                        } ?: this@StandardAlert.dismiss()
                    }
                }
//                (activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(etInput.getWindowToken(), 0)

            }
        }


    }

    fun cancelButton(cancelText: String, listener: ((StandardAlert) -> Unit)? = null) {
        this.cancelText = cancelText
        this.mOnCancel = listener
    }

    fun confirmButton(confirmText: String, listener: ((StandardAlert) -> Unit)? = null) {
        this.confirmText = confirmText
        this.mOnConfirm = listener
    }

    fun confirmButtonWithText(confirmText: String, listener: ((StandardAlert, String) -> Unit)? = null) {
        this.confirmText = confirmText
        this.mOnConfirmWithText = listener
    }

    fun confirmButtonWithDoubleEdit(confirmText: String, listener: ((StandardAlert, String, String) -> Unit)? = null) {
        this.confirmText = confirmText
        this.mOnConfirmWithDoubleEdit = listener
    }
}