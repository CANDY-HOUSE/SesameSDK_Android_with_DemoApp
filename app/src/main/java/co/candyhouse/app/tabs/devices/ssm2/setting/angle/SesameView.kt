package co.candyhouse.app.tabs.devices.ssm2.setting.angle

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import co.candyhouse.app.R
import co.candyhouse.sesame.open.device.CHSesame2
import co.candyhouse.sesame.open.device.CHSesame5
import kotlin.math.cos
import kotlin.math.sin

class SesameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var ssmImg: Bitmap =
        ContextCompat.getDrawable(context, R.drawable.img_knob_3x)!!.toBitmap()
    private var lockImg: Bitmap =
        ContextCompat.getDrawable(context, R.drawable.ic_icon_lock_uncheck)!!.toBitmap()
    private var unlockImg: Bitmap =
        ContextCompat.getDrawable(context, R.drawable.ic_icon_unlock_uncheck)!!.toBitmap()

    private var midx = 0f
    private var midy = 0f

    private var angle = 0f
    private var lockAngle = 0f
    private var unlockAngle = 0f

    private var knobSize = 0
    private var knobLeft = 0
    private var knobTop = 0

    private var iconSize = 0
    private var orbitRadius = 0f

    private fun reserveSpacePx(): Int = (iconSize + dp(12))

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val knobDesired = dp(220)

        iconSize = dp(28)

        val reserve = reserveSpacePx()

        val desiredSize = knobDesired + reserve * 2 + paddingLeft + paddingRight

        val w = resolveSize(desiredSize, widthMeasureSpec)
        val h = resolveSize(desiredSize, heightMeasureSpec)
        val size = minOf(w, h)

        setMeasuredDimension(size, size)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        midx = width / 2f
        midy = height / 2f

        iconSize = (width / 10).coerceIn(dp(22), dp(34))

        val reserve = reserveSpacePx()

        knobSize = (width - reserve * 2).coerceAtLeast(0)
        knobLeft = reserve
        knobTop = reserve

        orbitRadius = knobSize / 2f + iconSize / 2f + dp(6)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val rotateImg = ssmImg.rotate(angle)
        val imgZone = (rotateImg.width - ssmImg.width) / 2

        canvas.drawBitmap(
            rotateImg,
            Rect(imgZone, imgZone, imgZone + ssmImg.width, imgZone + ssmImg.width),
            Rect(knobLeft, knobTop, knobLeft + knobSize, knobTop + knobSize),
            null
        )

        drawIcon(canvas, lockImg, lockAngle, iconSize)
        drawIcon(canvas, unlockImg, unlockAngle, iconSize)
    }

    private fun drawIcon(canvas: Canvas, bmp: Bitmap, degFloat: Float, size: Int) {
        val rad = Math.toRadians(degFloat.toDouble())
        val cx = midx + cos(rad) * orbitRadius
        val cy = midy - sin(rad) * orbitRadius

        val left = (cx - size / 2f).toInt()
        val top = (cy - size / 2f).toInt()

        canvas.drawBitmap(
            bmp,
            Rect(0, 0, bmp.width, bmp.height),
            Rect(left, top, left + size, top + size),
            null
        )
    }

    fun setLock(ssm: CHSesame2) {
        if (ssm.mechSetting == null || ssm.mechStatus == null) return
        post {
            val degree = ssm.mechStatus!!.position.toFloat()
            val lockDegree = ssm.mechSetting!!.lockPosition.toFloat()
            val unlockDegree = ssm.mechSetting!!.unlockPosition.toFloat()

            angle = degree % 360
            lockAngle = lockDegree % 360
            unlockAngle = unlockDegree % 360
            invalidate()
        }
    }

    fun setLock(ssm: CHSesame5) {
        post {
            val degree = (ssm.mechStatus?.position ?: 0).toFloat()
            val lockDegree = (ssm.mechSetting?.lockPosition ?: 0).toFloat()
            val unlockDegree = (ssm.mechSetting?.unlockPosition ?: 0).toFloat()

            angle = degree % 360
            lockAngle = lockDegree % 360
            unlockAngle = unlockDegree % 360
            invalidate()
        }
    }
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply {
        setRotate(-degrees - 90)
    }
    density = DisplayMetrics.DENSITY_HIGH
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Float.toDG(): Double = Math.toRadians(this.toDouble())