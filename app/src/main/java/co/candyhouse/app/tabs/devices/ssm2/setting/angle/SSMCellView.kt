package co.candyhouse.app.tabs.devices.ssm2.setting.angle

import android.animation.ValueAnimator
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
import co.candyhouse.sesame.ble.Sesame2.CHDeviceLoginStatus
import co.candyhouse.sesame.utils.runOnUiThread
import co.utils.L
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class SSMCellView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var anim: ValueAnimator? = null
    private var ssmImg: Bitmap
    private var midx: Float? = null
    private var midy: Float? = null
    private var angle: Float = 0f

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
        lockMargin = ssmWidth / 2
        lockCenter = midx!! // must  x = y
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(ssmImg, Rect(0, 0, 0 + ssmImg.width, 0 + ssmImg.width), Rect(ssmMargin, ssmMargin, ssmMargin + ssmWidth, ssmMargin + ssmWidth), null)

        val lockdeg = angle.toDG()
        val lockMarginX = lockCenter + cos(lockdeg) * (lockMargin)
        val lockMarginY = lockCenter - sin(lockdeg) * (lockMargin)
        canvas.drawCircle(lockMarginX.toFloat(), lockMarginY.toFloat(), lockWidth.toFloat(), dotPaint)

    }

    fun setLockImage(ssm: CHSesame2) {
        ssmImg = ContextCompat.getDrawable(context, ssmUIParcer(ssm))!!.toBitmap()
        invalidate()
    }

    fun setLock(ssm: CHSesame2) {
        if (ssm.mechStatus == null) {
//            L.d("hcia", "ssm.mechStatus 擋掉了:" + ssm.mechStatus)
            return
        }


//        L.d("hcia", "ssm->target!!:" + ssm.mechStatus!!.target + " 動畫:" + anim?.isRunning)

        dotPaint.setColor(ContextCompat.getColor(context, if (ssm.mechStatus!!.isInLockRange) R.color.lock_red else R.color.unlock_blue))
        val degree = (ssm.mechStatus!!.position.toFloat() * 360 / 1024)
        val toTarget = ssm.mechStatus!!.target.toFloat() * 360 / 1024

        if (anim?.isRunning == true) {
            return
        }
//        L.d("hcia", "degree:" + degree + " toTarget:" + toTarget)

        if (ssm.mechStatus!!.target.toInt() == -32768) {
            post {
                anim = ValueAnimator.ofFloat(angle, degree)
                anim!!.duration = 500
                anim!!.addUpdateListener { animation ->
                    val currentValue = animation.animatedValue as Float
                    angle = currentValue
                    invalidate()
                }
                anim!!.start()
            }
        } else {
            post {
                anim = ValueAnimator.ofFloat(angle, toTarget)
                anim!!.duration = abs(toTarget.toLong() - angle.toLong()) * 10
                anim!!.addUpdateListener { animation ->
                    val currentValue = animation.animatedValue as Float
                    angle = currentValue
                    invalidate()
                }
                anim!!.start()
            }
        }


    }
}

