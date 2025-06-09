package co.utils.alerts

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import co.candyhouse.app.R


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
                findViewById<TextView>(R.id.tvTitle).text = it
                findViewById<TextView>(R.id.tvTitle).visibility = View.VISIBLE
            }
            contentText?.let {
                findViewById<TextView>(R.id.subTitle)    .text = it
                findViewById<TextView>(R.id.subTitle)   .visibility = View.VISIBLE
            }

        }
        update()

    }

    fun update() {

        findViewById<Button>(R.id.btnCancel).visibility = View.VISIBLE
        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            this@CheckAlert.dismiss()
//                mOnCancel?.invoke(this@CheckAlert) ?: this@CheckAlert.dismiss()
        }
    }


}