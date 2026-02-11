package co.candyhouse.app.tabs.devices.ssm2.setting.angle

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toColorInt
import co.candyhouse.app.R

class SlidingDoorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val lockImg: Bitmap =
        ContextCompat.getDrawable(context, R.drawable.ic_icon_lock_uncheck)!!.toBitmap()
    private val unlockImg: Bitmap =
        ContextCompat.getDrawable(context, R.drawable.ic_icon_unlock_uncheck)!!.toBitmap()

    private val paintInner = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#E7E6EB".toColorInt() }
    private val paintSlider = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#C2BEC6".toColorInt() }
    private val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FFFFFF".toColorInt()
        strokeWidth = dp(4).toFloat()
        strokeCap = Paint.Cap.ROUND
    }

    private val paintIcon = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

    private val iconSizePx: Float get() = dp(28).toFloat()

    private var progress: Float = 0f
    private var lockProgress: Float = 0.5f
    private var unlockProgress: Float = 0.5f

    private var observedMin: Float? = null
    private var observedMax: Float? = null

    private var lastLockPos: Int? = null
    private var lastUnlockPos: Int? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val bodyDesiredW = dp(240)
        val iconSafeAreaW = dp(70)
        val desiredW = bodyDesiredW + iconSafeAreaW

        val desiredH = dp(360)

        val w = resolveSize(desiredW + paddingLeft + paddingRight, widthMeasureSpec)
        val h = resolveSize(desiredH + paddingTop + paddingBottom, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        val bodyW = dp(130).toFloat()

        val bodyTop = dp(20).toFloat()
        val bodyBottom = h - dp(20)

        val bodyLeft = w / 2f - bodyW / 2f
        val bodyRight = bodyLeft + bodyW

        val radius = dp(22).toFloat()

        val innerRect = RectF(bodyLeft, bodyTop, bodyRight, bodyBottom)
        canvas.drawRoundRect(innerRect, radius * 0.80f, radius * 0.80f, paintInner)

        val sliderH = innerRect.height() * 0.42f
        val trackTop = innerRect.top
        val trackBottom = innerRect.bottom - sliderH

        val sliderSidePad = dp(6).toFloat()
        val sliderTop = trackTop + (trackBottom - trackTop) * (1f - progress)
        val sliderRect = RectF(
            innerRect.left + sliderSidePad,
            sliderTop,
            innerRect.right - sliderSidePad,
            sliderTop + sliderH
        )
        canvas.drawRoundRect(sliderRect, radius * 0.9f, radius * 0.9f, paintSlider)

        val lineY = sliderRect.centerY()
        val lineStartX = sliderRect.left + sliderRect.width() * 0.18f
        val lineEndX = sliderRect.right - sliderRect.width() * 0.18f
        canvas.drawLine(lineStartX, lineY, lineEndX, lineY, paintLine)

        val iconX = innerRect.right + dp(18)
        drawIconByProgress(canvas, unlockImg, unlockProgress, iconX, trackTop, trackBottom, sliderH)
        drawIconByProgress(canvas, lockImg, lockProgress, iconX, trackTop, trackBottom, sliderH)
    }

    private fun drawIconByProgress(
        canvas: Canvas,
        bmp: Bitmap,
        pRaw: Float,
        x: Float,
        trackTop: Float,
        trackBottom: Float,
        sliderH: Float
    ) {
        val p = pRaw.coerceIn(0f, 1f)
        val y = trackTop + (trackBottom - trackTop) * (1f - p) + sliderH / 2f
        drawIconAt(canvas, bmp, x, y)
    }

    private fun drawIconAt(canvas: Canvas, bmp: Bitmap, leftX: Float, centerY: Float) {
        val size = iconSizePx
        val dst = RectF(
            leftX,
            centerY - size / 2f,
            leftX + size,
            centerY + size / 2f
        )
        canvas.drawBitmap(bmp, null, dst, paintIcon)
    }

    fun setLock(pos: Int, lockPos: Int, unlockPos: Int) {
        post {
            val p = pos.toFloat()
            val lp = lockPos.toFloat()
            val up = unlockPos.toFloat()

            observedMin = listOfNotNull(observedMin, p, lp, up).minOrNull()
            observedMax = listOfNotNull(observedMax, p, lp, up).maxOrNull()

            var min = observedMin ?: p
            var max = observedMax ?: p

            val minSpan = 20f
            if (max - min < minSpan) {
                val c = (max + min) / 2f
                min = c - minSpan / 2f
                max = c + minSpan / 2f
            }

            progress = normalize(p, min, max)

            if (lastLockPos == null || lastLockPos != lockPos) {
                lockProgress = normalize(lp, min, max)
                lastLockPos = lockPos
            }
            if (lastUnlockPos == null || lastUnlockPos != unlockPos) {
                unlockProgress = normalize(up, min, max)
                lastUnlockPos = unlockPos
            }

            invalidate()
        }
    }

    private fun normalize(v: Float, min: Float, max: Float): Float {
        if (max - min == 0f) return 0.5f
        val clamped = v.coerceIn(min, max)
        return (clamped - min) / (max - min)
    }
}