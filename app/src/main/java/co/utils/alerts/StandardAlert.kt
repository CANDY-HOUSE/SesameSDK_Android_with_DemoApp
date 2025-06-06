package co.utils.alerts

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView

import co.candyhouse.app.R

import co.utils.alerts.ext.AlertType
import co.utils.alerts.ext.InputType
import co.utils.materialtextfield.MaterialTextField

class StandardAlert(context: Context, val type: AlertType) : BaseAlert(context) {

    var titleText: String? = null
    var lastName: String? = null
    var firstName: String? = null
    var topHinText: String? = null
    var secondHinText: String? = null
    var inputType = InputType.Text
    var hintText:String?=null
    var hintColor:Int?=null

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
            AlertType.Normal -> findViewById<Button>(R.id.btnConfirm).setBackgroundResource(R.drawable.bg_btn_confirm)
            AlertType.Warning -> findViewById<Button>(R.id.btnConfirm).setBackgroundResource(R.drawable.bg_btn_warning)
            AlertType.InputText -> {
                hintText?.apply {


                    findViewById<MaterialTextField>(R.id.etInput) .hint = this
                }
                hintColor?.apply {
                    findViewById<MaterialTextField>(R.id.etInput).setHintTextColor(this)

                }
                lastName?.apply {
                    findViewById<MaterialTextField>(R.id.etInput).setText(this)
                }

                findViewById<MaterialTextField>(R.id.etInput).inputType = inputType.value
                findViewById<MaterialTextField>(R.id.etInput__2)     .visibility = View.GONE
                findViewById<View>(R.id.hintl2)      .visibility = View.GONE
                findViewById<TextView>(R.id.hint_second_tx)       .visibility = View.GONE
                findViewById<TextView>(R.id.hint_top)       .text = topHinText
//                hint_top.visibility = View.GONE
            }
            AlertType.TwoEdit -> {
//                etInput.hint = hintText
                findViewById<MaterialTextField>(R.id.etInput).setText(lastName)
                findViewById<MaterialTextField>(R.id.etInput__2)    .setText(firstName)

                findViewById<MaterialTextField>(R.id.etInput).inputType = inputType.value
                findViewById<MaterialTextField>(R.id.etInput__2) .inputType = inputType.value
                findViewById<MaterialTextField>(R.id.etInput).visibility = View.VISIBLE
                findViewById<MaterialTextField>(R.id.etInput__2) .visibility = View.VISIBLE
                findViewById<TextView>(R.id.hint_top).text = topHinText
                findViewById<TextView>(R.id.hint_second_tx)     .text = secondHinText

            }
            else -> {}
        }


    }

    fun update() {

        titleText?.let {
            findViewById<TextView>(R.id.tvTitle)      .text = it
            findViewById<TextView>(R.id.tvTitle) .visibility = View.VISIBLE
        }

//        contentText?.let {
//        }

        cancelText?.let {
//            btnCancel.text = it
            findViewById<Button>(R.id.btnCancel)        .visibility = View.VISIBLE
            findViewById<Button>(R.id.btnCancel).setOnClickListener {
                mOnCancel?.invoke(this@StandardAlert) ?: this@StandardAlert.dismiss()
            }
        }

        confirmText?.let {
            findViewById<Button>(R.id.btnConfirm)         .text = it
            findViewById<Button>(R.id.btnConfirm) .visibility = View.VISIBLE
            findViewById<Button>(R.id.btnConfirm) .setOnClickListener {
//                L.d("hcia", "type:" + type)
                when (type) {
                    AlertType.TwoEdit -> {

                        if (    findViewById<MaterialTextField>(R.id.etInput).text.isNullOrBlank()
                                ||    findViewById<MaterialTextField>(R.id.etInput__2) .text.isNullOrBlank()) {
                            val shake = AnimationUtils.loadAnimation(context, R.anim.shake)
                            findViewById<MaterialTextField>(R.id.etInput)        .startAnimation(shake)
                        } else {
                            mOnConfirmWithDoubleEdit?.invoke(this@StandardAlert,
                                    findViewById<MaterialTextField>(R.id.etInput) .text.toString(),
                                    findViewById<MaterialTextField>(R.id.etInput__2)         .text.toString())
                        }
                    }
                    else -> {

                        mOnConfirm?.invoke(this@StandardAlert) ?: mOnConfirmWithText?.let {
                            if (findViewById<MaterialTextField>(R.id.etInput).text.isNullOrBlank()) {
                                val shake = AnimationUtils.loadAnimation(context, R.anim.shake)
                                findViewById<MaterialTextField>(R.id.etInput).startAnimation(shake)
                            } else {
                                mOnConfirmWithText?.invoke(this@StandardAlert, findViewById<MaterialTextField>(R.id.etInput).text.toString())
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