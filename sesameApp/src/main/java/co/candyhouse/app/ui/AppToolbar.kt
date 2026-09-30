package co.candyhouse.app.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import co.candyhouse.app.R
import co.candyhouse.app.util.dp

@SuppressLint("ViewConstructor")
class AppToolbar(context: Context, onBack: () -> Unit, onMenu: () -> Unit, onInternalTest: () -> Unit) : FrameLayout(context) {
    val menu = TextView(context).apply {
        text = "⊕"; textSize = 32f; gravity = Gravity.CENTER; setTextColor(Color.BLACK)
        contentDescription = context.getString(R.string.add_new_device)
        setOnClickListener { onMenu() }
    }
    private val back = ImageView(context).apply {
        setImageResource(R.drawable.ic_arrow); rotation = 180f; scaleType = ImageView.ScaleType.CENTER
        contentDescription = context.getString(R.string.back)
        setOnClickListener { onBack() }
    }
    private val testTrigger = View(context).apply {
        contentDescription = "内部测试"
        setOnLongClickListener { onInternalTest(); true }
    }

    init {
        setBackgroundColor(Color.WHITE)
        addView(back, LayoutParams(context.dp(48), -1, Gravity.START))
        addView(menu, LayoutParams(context.dp(48), -1, Gravity.END).apply { marginEnd = context.dp(8) })
        addView(testTrigger, LayoutParams(context.dp(120), -1, Gravity.CENTER_HORIZONTAL))
        showPage(root = true, home = false)
    }

    fun showPage(root: Boolean, home: Boolean) {
        back.visibility = if (root) GONE else VISIBLE
        menu.visibility = if (root) VISIBLE else GONE
        testTrigger.visibility = if (home) VISIBLE else GONE
    }
}
