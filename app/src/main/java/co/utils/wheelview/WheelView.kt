package co.utils.wheelview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.OverScroller
import co.candyhouse.app.R
import com.victor.library.wheelview.mode.IWheelViewMode
import com.victor.library.wheelview.mode.WheelViewRecycleMode

/**
 * Created by Victor on 2017/6/12.
 */

class WheelView<T> : View {
    private val TAG = "WheelView"
    private val SHADOWS_COLORS = intArrayOf(0xefffffff.toInt(), 0xcfffffff.toInt(), 0x3fffffff)
    private var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var textColor: Int = 0x000
    private var textSize: Float = 19f
    private var bgColor: Int = 0x000
    private lateinit var scroller: OverScroller
    public var eachItemHeight = 60
    private var maxShowSize = 7
    private var prevY: Float = 0f

    // Shadows drawables
    private var topShadow: GradientDrawable? = null
    private var bottomShadow: GradientDrawable? = null

    private var gestureDetector: GestureDetector? = null
    private var wheelScrollListener: WheelScrollListener? = null

    private var mSelected: Int = 0

    private var canDragOutBorder = true
    private var mScrollY: Int = 0
    private var vTracker: VelocityTracker? = null
    private var mEdgeSlop: Float = 0f
    private var mFlingDistance: Int = 0
    private var mCurrY: Int = 0
    private lateinit var adapter: IWheelviewAdapter

    /**
     * 模式:居中显示；从起始位置显示；循环显示
     */
    private var wheelViewMode: IWheelViewMode = WheelViewRecycleMode(eachItemHeight, 0)


    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        initParams(context, attrs, defStyleAttr, defStyleRes)
    }

    fun initParams(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val typedArray = context!!.theme.obtainStyledAttributes(attrs, R.styleable.WheelView, defStyleAttr, defStyleRes)
        val count = typedArray.indexCount
        for (i in 0..count) {
            val attr = typedArray.getIndex(i)
            when (attr) {
                R.styleable.WheelView_textColor -> textColor = typedArray.getColor(attr, 0x000)
                R.styleable.WheelView_textSize -> textSize = typedArray.getDimension(attr, 19f)
                R.styleable.WheelView_dragOut -> canDragOutBorder = typedArray.getBoolean(attr, true)
            }
        }
        paint.color = textColor
        paint.textSize = textSize
        typedArray.recycle()
    }

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            var dy: Int = (-vTracker!!.yVelocity / 8).toInt()
            // 边距检测
            if (scrollY + dy <= wheelViewMode.getTopMaxScrollHeight()) {
                dy = wheelViewMode.getTopMaxScrollHeight() - scrollY
            }
            if (scrollY + dy >= wheelViewMode.getBottomMaxScrollHeight()) {
                dy = wheelViewMode.getBottomMaxScrollHeight() - scrollY
            }
            // 取整每次onfling的距离
            var scrollDy = scrollY % eachItemHeight
            if (scrollDy + dy % eachItemHeight > eachItemHeight / 2) {
                dy += eachItemHeight - (scrollDy + dy % eachItemHeight)
            } else if (scrollDy + dy % eachItemHeight > 0 && scrollDy + dy % eachItemHeight <= eachItemHeight / 2) {
                dy -= scrollDy + dy % eachItemHeight
            } else if (scrollDy + dy % eachItemHeight < 0 && scrollDy + dy % eachItemHeight >= -eachItemHeight / 2) {
                dy -= scrollDy + dy % eachItemHeight
            } else if (scrollDy + dy % eachItemHeight < -eachItemHeight / 2) {
                dy -= scrollDy + dy % eachItemHeight + eachItemHeight
            }
            mFlingDistance = dy
            Log.e(TAG, "onfling: scrollY = ${scrollY} --- dy = $dy -- scrollY + dy = ${scrollY + dy}")
            scroller.startScroll(0, scrollY, 0, dy, 600)
            invalidate()
            return true
        }
    }

    private val task = Runnable {
        var moveIndex: Int = ((mCurrY + mFlingDistance) * 1f / eachItemHeight).toInt()
        var selected = wheelViewMode.getSelectedIndex(moveIndex)
        Log.e(TAG, "currY = $mCurrY -- mFlingDistance = $mFlingDistance -- moveIndex = $moveIndex  -- selected = $selected")
        if (mSelected != selected) {
            mSelected = selected
            wheelScrollListener?.changed(mSelected, adapter.get(mSelected))
        }
    }

    init {
        mEdgeSlop = 1f
        bgColor = resources.getColor(R.color.white)
        parent?.requestDisallowInterceptTouchEvent(true)
        scroller = OverScroller(context, AccelerateDecelerateInterpolator())
        gestureDetector = GestureDetector(context, gestureListener)

        if (topShadow == null) {
            topShadow = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, SHADOWS_COLORS)
        }
        if (bottomShadow == null) {
            bottomShadow = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, SHADOWS_COLORS)
        }
    }

    fun getAdapter(): IWheelviewAdapter {
        return adapter
    }

    fun setAdapter(adapter: IWheelviewAdapter) {
        this.adapter = adapter
        wheelViewMode.childrenSize = (adapter.count)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var widthMOde = MeasureSpec.getMode((widthMeasureSpec))
        var heightM = MeasureSpec.getSize(heightMeasureSpec)
        var widthM = MeasureSpec.getSize(widthMeasureSpec)

        if (heightMode == MeasureSpec.EXACTLY && widthMOde == MeasureSpec.EXACTLY) {
            setMeasuredDimension(widthM, heightM)
        } else if (heightMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(600, heightM)
        } else if (widthMOde == MeasureSpec.EXACTLY) {
            setMeasuredDimension(widthM, 600)
        } else {
            setMeasuredDimension(400, 600)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mFlingDistance = 0
                if (vTracker == null) {
                    vTracker = VelocityTracker.obtain()
                } else {
                    vTracker?.clear()
                }

                if (adapter.count == 0) {
                    return false
                }

                val parent = parent
                parent?.requestDisallowInterceptTouchEvent(true)

                if (!scroller.isFinished) {
                    handler.removeCallbacks(task)
                    scroller.forceFinished(true)
                }
                prevY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                vTracker?.addMovement(event)
                vTracker?.computeCurrentVelocity(1000)
                var currY = event.y
                var dy: Int = (currY - prevY).toInt()
                if (Math.abs(dy) > mEdgeSlop) {
                    if (!canDragOutBorder) {
                        if (scrollY + dy <= 0 && scrollY + dy <= wheelViewMode.getTopMaxScrollHeight()) {
                            dy -= wheelViewMode.getTopMaxScrollHeight() - scrollY
                        } else if (scrollY + dy > 0 && scrollY + dy >= wheelViewMode.getBottomMaxScrollHeight()) {
                            dy -= wheelViewMode.getBottomMaxScrollHeight() - scrollY
                        }
                    }
                    Log.e(TAG, "currY = $currY  -- prevY = $prevY -- scrollY = $scrollY -- dy = $dy")
                    scrollBy(0, -dy)
                    invalidate()
                }
                prevY = currY
            }
            MotionEvent.ACTION_UP -> {
                scrollerStop()
            }
        }
        gestureDetector?.onTouchEvent(event)
        return true
    }

    private fun scrollerStop() {
        var offset = if (scrollY < 0) -0.5f else 0.5f
        var moveIndex: Int = (scrollY * 1f / eachItemHeight + offset).toInt()
        var dy: Int = scrollY - moveIndex * eachItemHeight

        if (canDragOutBorder && wheelViewMode !is WheelViewRecycleMode) {
            if (scrollY <= 0 && scrollY <= wheelViewMode.getTopMaxScrollHeight()) {
                dy = -wheelViewMode.getTopMaxScrollHeight() + scrollY
            } else if (scrollY > 0 && scrollY >= wheelViewMode.getBottomMaxScrollHeight()) {
                dy = scrollY - wheelViewMode.getBottomMaxScrollHeight()
            }
        }
        mCurrY = scrollY
        mFlingDistance = -dy
        scroller.startScroll(0, scrollY, 0, -dy, 200)
        invalidate()
        handler.postDelayed(task, 200)

    }

    fun setWheelScrollListener(wheelScrollListener: WheelScrollListener) {
        this.wheelScrollListener = wheelScrollListener
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            mScrollY = scroller.currY
            scrollTo(scroller.currX, scroller.currY)
            postInvalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (adapter.count == 0) return
        canvas.drawColor(bgColor)
        clipView(canvas)
        drawText(canvas)
        drawLine(canvas)
        drawShadows(canvas)
    }

    private fun drawLine(canvas: Canvas) {
        val baseHeight: Float = (height - eachItemHeight) / 2.toFloat()
        paint.color = resources.getColor(R.color.gray2)
        canvas.drawLine(0f, baseHeight + scrollY, width.toFloat(), baseHeight + scrollY, paint)
        canvas.drawLine(0f, baseHeight + eachItemHeight + scrollY, width.toFloat(),
                baseHeight + eachItemHeight + scrollY, paint)
    }

    private fun drawText(canvas: Canvas) {
        paint.color = resources.getColor(R.color.black)
        var y: Float
        var x: Float
        var minStart: Int = 0
        var count: Int = adapter.count
        if (wheelViewMode is WheelViewRecycleMode) {
            minStart = -maxShowSize / 2 - 1 + scrollY / eachItemHeight // 多显示一个，避免闪现
            count = adapter.count + scrollY / eachItemHeight
        }
        var index: Int = 0
        for (i in minStart until count step 1) {
            index = i
            if (wheelViewMode is WheelViewRecycleMode && index < 0) {
                while (index < 0) {
                    index += adapter.count
                }
            } else {
                index = i
            }
            x = (width - paint.measureText(adapter.getItemeTitle(index % adapter.count))) / 2
            y = wheelViewMode.getTextDrawY(height, i, paint)
            canvas.drawText(adapter.getItemeTitle(index % adapter.count), x, y, paint)
        }
    }

    private fun getMaxHeight(): Float {
        var maxHeight: Float = eachItemHeight * maxShowSize.toFloat()
        if (maxHeight > height) {
            maxHeight = height.toFloat()
        }
        return maxHeight
    }

    private fun clipView(canvas: Canvas) {
        var reqHeight: Float = getMaxHeight()
        var starOffset: Float = (height - reqHeight) / 2f
        canvas.clipRect(0f, starOffset + scrollY, width.toFloat(),
                starOffset + reqHeight + scrollY)
    }

    private fun getShadowsHeight(): Int {
        return (height - eachItemHeight) / 2
    }

    private fun drawShadows(canvas: Canvas) {
        val height = getShadowsHeight()
        topShadow?.setBounds(0, scrollY, width, height + scrollY)
        topShadow?.draw(canvas)

        bottomShadow?.setBounds(0, scrollY + eachItemHeight + height, width, getHeight() + scrollY)
        bottomShadow?.draw(canvas)
    }

    fun setMode(mode: IWheelViewMode) {
        this.wheelViewMode = mode
        scrollTo(0, 0)
        invalidate()
    }

    fun getMode() = this.wheelViewMode

    fun getContentSize() = adapter.count

    fun getMaxShowSize() = maxShowSize

    fun setMaxShowSize(size: Int) {
        maxShowSize = size
    }


    interface WheelScrollListener {
        fun changed(selected: Int, name: Any?)
    }

}
