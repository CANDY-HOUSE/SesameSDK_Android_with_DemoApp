package co.candyhouse.app.tabs.devices.ssm2.setting.angle

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import co.candyhouse.sesame.ble.Sesame2.CHSesame2
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.ssm2.ssmUIParcer
import co.candyhouse.sesame.ble.CHDeviceLoginStatus
import kotlin.math.cos
import kotlin.math.sin

class SSMCellView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var ssmImg: Bitmap
    private var midx: Float? = null
    private var midy: Float? = null
    private var angle: Float = 0f
    private var lockAngle: Float = 0f
    private var unlockAngle: Float = 0f

    var ssmWidth: Int = 0
    var ssmMargin: Int = 0
    var lockWidth: Int = 0
    var lockMargin: Int = 0
    var lockCenter: Float = 0f
    var dotPaint: Paint

    init {

        ssmImg = ContextCompat.getDrawable(context, R.drawable.icon_nosignal)!!.toBitmap()
        dotPaint = Paint()
        dotPaint.setColor(ContextCompat.getColor(context, R.color.clear))
//        dotPaint.setStrokeWidth(30F)
        dotPaint.setStyle(Paint.Style.FILL)
        dotPaint.setAntiAlias(true)
        dotPaint.setDither(true)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        midx = width / 2.toFloat()
        midy = height / 2.toFloat()

        ssmWidth = width * 8 / 10
        ssmMargin = (width - ssmWidth) / 2

        lockWidth = width / 30
        lockMargin = ssmWidth / 2 + lockWidth
        lockCenter = midx!! // must  x = y
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

//        java.lang.RuntimeException: Canvas: trying to draw too large(260305956bytes) bitmap.
        canvas.drawBitmap(ssmImg, Rect(0, 0, 0 + ssmImg.width, 0 + ssmImg.width), Rect(ssmMargin, ssmMargin, ssmMargin + ssmWidth, ssmMargin + ssmWidth), null)

        val lockdeg = angle.toDG()
        val lockMarginX = lockCenter + cos(lockdeg) * (lockMargin)
        val lockMarginY = lockCenter - sin(lockdeg) * (lockMargin)

        canvas.drawCircle(lockMarginX.toFloat(), lockMarginY.toFloat(), lockWidth.toFloat(), dotPaint)

    }

    fun setLock(ssm: CHSesame2) {
//        ssmImg = getResources().getDrawable(ssmUIParcer(ssm)).toBitmap()
        ssmImg = ContextCompat.getDrawable(context, ssmUIParcer(ssm))!!.toBitmap()

        if (ssm.deviceStatus.value == CHDeviceLoginStatus.unlogined) {
            dotPaint.setColor(ContextCompat.getColor(context, R.color.clear))

        } else {
//            L.d("hcia", "ssm.mechStatus:" + ssm.mechStatus)
            dotPaint.setColor(ContextCompat.getColor(context, if (ssm.mechStatus!!.inLockRange) R.color.lock_red else R.color.unlock_blue)) //todo crask  mechStatus null
            ssm.mechSetting?.unlockPosition
            val degree = ssm.mechStatus!!.position.toFloat() * 360 / 1024
            angle = degree % 360
        }
        invalidate()
    }
}

