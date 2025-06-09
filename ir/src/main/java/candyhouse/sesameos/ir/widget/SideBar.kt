package candyhouse.sesameos.ir.widget


import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import candyhouse.sesameos.ir.R


class SideBar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : View(context, attrs, defStyle) {

    // 触摸事件
    private var onTouchingLetterChangedListener: OnTouchingLetterChangedListener? = null

    // 26个字母
    companion object {
        val direction = mutableListOf(
                "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "#"
        )
    }

    private var choose = -1 // 选中
    private val paint = Paint()

    private var mTextDialog: TextView? = null

    fun setTextView(mTextDialog: TextView) {
        this.mTextDialog = mTextDialog
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat() // 获取视图宽度
        val height = height.toFloat() // 获取视图高度

        // 计算单个字母的高度
        val singleHeight = 50f

        // 计算所有字母总共占用的高度
        val totalLettersHeight = singleHeight * direction.size

        // 计算起始Y坐标，使内容居中
        val startY = (height - totalLettersHeight) / 2

        for (i in direction.indices) {
            paint.apply {
                color = ContextCompat.getColor(context, R.color.text_blue)
                typeface = Typeface.DEFAULT
                isAntiAlias = true
                textSize = 35f

                // 选中状态
                if (i == choose) {
                    color = Color.parseColor("#ffffff")
                    isFakeBoldText = true
                }

                // 计算x坐标（水平居中）
                val xPos = width / 2 - measureText(direction[i]) / 2

                // 计算y坐标（考虑起始位置）
                val yPos = startY + singleHeight * i + singleHeight

                // 绘制文字
                canvas.drawText(direction[i], xPos, yPos, this)
                reset() // 重置画笔
            }
        }
    }

    // 同时需要修改触摸事件的计算逻辑
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        val y = event.y
        val oldChoose = choose
        val listener = onTouchingLetterChangedListener

        // 计算总高度和起始位置
        val singleHeight = 50f
        val totalLettersHeight = singleHeight * direction.size
        val startY = (height - totalLettersHeight) / 2

        // 调整触摸位置的计算
        val adjustedY = y - startY
        val c = if (adjustedY < 0) {
            0
        } else if (adjustedY > totalLettersHeight) {
            direction.size - 1
        } else {
            (adjustedY / singleHeight).toInt()
        }.coerceIn(0, direction.size - 1)

        when (action) {
            MotionEvent.ACTION_UP -> {
                background = ColorDrawable(0x00000000)
                choose = -1
                invalidate()
                mTextDialog?.visibility = View.INVISIBLE
            }
            else -> {
                setBackgroundResource(R.drawable.sidebar_background)
                if (oldChoose != c) {
                    listener?.onTouchingLetterChanged(direction[c])
                    mTextDialog?.apply {
                        text = direction[c]
                        visibility = View.VISIBLE
                    }
                    choose = c
                    invalidate()
                }
            }
        }
        return true
    }

    /**
     * 向外公开的方法
     *
     * @param onTouchingLetterChangedListener
     */
    fun setOnTouchingLetterChangedListener(onTouchingLetterChangedListener: OnTouchingLetterChangedListener) {
        this.onTouchingLetterChangedListener = onTouchingLetterChangedListener
    }
    fun setOnTouchingLetterChangedListener(  onclick: (String) -> Unit={}) {
        this.onTouchingLetterChangedListener =object :OnTouchingLetterChangedListener{
            override fun onTouchingLetterChanged(s: String) {
                onclick(s)
            }
        }
    }

    fun setDirection(realDirection: ArrayList<String>) {
        direction.clear()
        direction.addAll(realDirection)
        invalidate()
    }
    /**
     * 接口
     *
     * @author coder
     */
    interface OnTouchingLetterChangedListener {
        fun onTouchingLetterChanged(s: String)
    }
}
