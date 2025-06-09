package candyhouse.sesameos.ir.adapter



import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.RecyclerView

class IrAppDivider (
        context: Context,
        private val leftMargin: Int,
        private val rightMargin: Int
    ) : RecyclerView.ItemDecoration() {

        private val dividerDrawable: Drawable?

        init {
            // 获取分割线Drawable
            val typedArray = context.obtainStyledAttributes(intArrayOf(android.R.attr.listDivider))
            dividerDrawable = typedArray.getDrawable(0)
            typedArray.recycle()
        }

        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            if (dividerDrawable == null) return

            val left = parent.paddingLeft + leftMargin
            val right = parent.width - parent.paddingRight - rightMargin

            val childCount = parent.childCount
            for (i in 0 until childCount - 1) { // 不在最后一个item下画分割线
                val child = parent.getChildAt(i)
                val params = child.layoutParams as RecyclerView.LayoutParams

                val top = child.bottom + params.bottomMargin
                val bottom = top + dividerDrawable.intrinsicHeight

                dividerDrawable.setBounds(left, top, right, bottom)
                dividerDrawable.draw(c)
            }
        }
    }