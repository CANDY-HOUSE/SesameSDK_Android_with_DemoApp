package co.utils

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.isNotEmpty
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlin.math.abs

/**
 * 自定义下拉刷新控件
 *
 * @author frey on 2026/2/6
 */
class SmartSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    var scrollTarget: View? = null

    private val excluded = LinkedHashSet<View>()
    private val tmpRect = Rect()

    fun addExcludedView(view: View) {
        excluded.add(view)
    }

    fun removeExcludedView(view: View) {
        excluded.remove(view)
    }

    fun clearExcludedViews() {
        excluded.clear()
    }

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var disallowInterceptBecauseHorizontal = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN || ev.actionMasked == MotionEvent.ACTION_MOVE) {
            if (isInExcludedArea(ev)) return false
        }

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                disallowInterceptBecauseHorizontal = false
                return super.onInterceptTouchEvent(ev)
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = abs(ev.x - downX)
                val dy = abs(ev.y - downY)

                if (!disallowInterceptBecauseHorizontal && dx > touchSlop && dx > dy) {
                    disallowInterceptBecauseHorizontal = true
                }
                if (disallowInterceptBecauseHorizontal) return false

                val target = resolveScrollTarget()
                if (target != null && ViewCompat.canScrollVertically(target, -1)) {
                    return false
                }

                return super.onInterceptTouchEvent(ev)
            }

            else -> return super.onInterceptTouchEvent(ev)
        }
    }

    private fun isInExcludedArea(ev: MotionEvent): Boolean {
        if (excluded.isEmpty()) return false
        val x = ev.rawX.toInt()
        val y = ev.rawY.toInt()
        for (v in excluded) {
            if (!v.isShown) continue
            v.getGlobalVisibleRect(tmpRect)
            if (tmpRect.contains(x, y)) return true
        }
        return false
    }

    private fun resolveScrollTarget(): View? {
        scrollTarget?.let { return it }
        val content = if (isNotEmpty()) getChildAt(0) else null
        return content?.let { findFirstScrollableChild(it) } ?: content
    }

    private fun findFirstScrollableChild(view: View): View? {
        if (ViewCompat.canScrollVertically(view, -1) || ViewCompat.canScrollVertically(view, 1)) {
            return view
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val target = findFirstScrollableChild(view.getChildAt(i))
                if (target != null) return target
            }
        }
        return null
    }
}