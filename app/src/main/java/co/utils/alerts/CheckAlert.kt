package co.utils.alerts

import android.content.Context
import android.os.Bundle
import android.view.View
import co.candyhouse.app.R
import kotlinx.android.synthetic.main.alert_check.*


class CheckAlert(context: Context) : BaseAlert(context) {

    var titleText: String? = null
    var contentText: String? = null

    private var cancelText: String? = null

    private var mOnCancel: ((CheckAlert) -> Unit)? = null

    override val layout: Int get() = R.layout.alert_check

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(mAlertView) {

            titleText?.let {
                tvTitle.text = it
                tvTitle.visibility = View.VISIBLE
            }
            contentText?.let {
                subTitle.text = it
                subTitle.visibility = View.VISIBLE
            }

        }
        update()

    }

    fun update() {
        btnCancel.visibility = View.VISIBLE
        btnCancel.setOnClickListener {
            this@CheckAlert.dismiss()
//                mOnCancel?.invoke(this@CheckAlert) ?: this@CheckAlert.dismiss()
        }
    }


}