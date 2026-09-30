package co.candyhouse.app.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import co.candyhouse.app.R
import co.candyhouse.app.util.dp

class AppMenu(private val context: Context, private val register: () -> Unit, private val scan: () -> Unit, private val contact: () -> Unit) {
    private var actionMenu: PopupWindow? = null
    fun dismiss() {
        actionMenu?.dismiss()
    }

    fun show(anchor: View) {
        dismiss()
        val menuColor = Color.rgb(68, 68, 68)
        val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        content.addView(ImageView(context).apply {
            setImageResource(R.drawable.arrow)
            setColorFilter(menuColor)
        }, LinearLayout.LayoutParams(context.dp(12), context.dp(12)).apply {
            gravity = Gravity.END
            marginEnd = context.dp(18)
        })
        val items = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { setColor(menuColor); cornerRadius = context.dp(4).toFloat() }
        }
        content.addView(items)
        val popup = PopupWindow(content, context.dp(180), -2, true).apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            isOutsideTouchable = true
        }

        fun addItem(title: Int, icon: Int, action: () -> Unit) {
            items.addView(TextView(context).apply {
                setText(title)
                textSize = 15f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(context.dp(24), context.dp(18), context.dp(16), context.dp(18))
                compoundDrawablePadding = context.dp(10)
                val drawable = context.getDrawable(icon)?.apply { setBounds(0, 0, context.dp(24), context.dp(24)) }
                setCompoundDrawablesRelative(drawable, null, null, null)
                setOnClickListener { popup.dismiss(); action() }
            }, LinearLayout.LayoutParams(-1, -2))
        }
        addItem(R.string.add_new_device, R.drawable.ic_cube, register)
        addItem(R.string.scan_the_qr_code, R.drawable.ic_qr_code_scan, scan)
        addItem(R.string.add_contacts, R.drawable.ic_add_contact, contact)
        actionMenu = popup
        popup.setOnDismissListener { actionMenu = null }
        popup.showAsDropDown(anchor, 0, 0, Gravity.END)
    }

}
