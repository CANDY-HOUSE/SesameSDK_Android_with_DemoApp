package co.candyhouse.app.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import co.candyhouse.app.R
import co.candyhouse.app.util.dp

/** Back row above an external page only; Biz draws every header of its own pages. */
@SuppressLint("ViewConstructor")
class AppToolbar(context: Context, onBack: () -> Unit) : FrameLayout(context) {
    private val back = ImageView(context).apply {
        setImageResource(R.drawable.ic_arrow); rotation = 180f; scaleType = ImageView.ScaleType.CENTER
        contentDescription = context.getString(R.string.back)
        setOnClickListener { onBack() }
    }

    init {
        setBackgroundColor(Color.WHITE)
        addView(back, LayoutParams(context.dp(48), -1, Gravity.START))
    }
}
