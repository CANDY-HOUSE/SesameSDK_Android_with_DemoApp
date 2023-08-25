package co.candyhouse.app.tabs.devices.ssm2.setting.angle

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import co.candyhouse.app.R
import co.candyhouse.sesame.open.device.CHSesame2
import co.candyhouse.sesame.open.device.CHSesame5
import co.candyhouse.sesame.open.device.CHSesameBike2
import co.utils.L
import kotlin.math.cos
import kotlin.math.sin
import android.util.DisplayMetrics as DisplayMetrics

class SesameView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private var ssmImg: Bitmap
    private var lockImg: Bitmap
    private var unlockImg: Bitmap
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

    init {
        ssmImg = ContextCompat.getDrawable(context, R.drawable.img_knob_3x)!!.toBitmap()
        lockImg = ContextCompat.getDrawable(context, R.drawable.ic_icon_lock_uncheck)!!.toBitmap()
        unlockImg = ContextCompat.getDrawable(context, R.drawable.ic_icon_unlock_uncheck)!!.toBitmap()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        midx = width / 2.toFloat()
        midy = height / 2.toFloat()

        ssmWidth = width / 2
        ssmMargin = (width - ssmWidth) / 2

        lockWidth = width / 15
        lockMargin = ssmWidth / 2 + lockWidth
        lockCenter = midx!! - lockWidth / 2 // must  x = y
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val rotateImg = ssmImg.rotate(angle)
        val imgZone = (rotateImg.width - ssmImg.width) / 2
        canvas.drawBitmap(rotateImg, Rect(imgZone, imgZone, imgZone + ssmImg.width, imgZone + ssmImg.width), Rect(ssmMargin, ssmMargin, ssmMargin + ssmWidth, ssmMargin + ssmWidth), null)

        val lockdeg = lockAngle.toDG()
        val lockMarginX = lockCenter + cos(lockdeg) * (lockMargin)
        val lockMarginY = lockCenter - sin(lockdeg) * (lockMargin)
        canvas.drawBitmap(lockImg, Rect(0, 0, 0 + lockImg.width, 0 + lockImg.width), Rect(lockMarginX.toInt(), lockMarginY.toInt(), lockMarginX.toInt() + lockWidth, lockMarginY.toInt() + lockWidth), null)
        val unlockdeg = unlockAngle.toDG()
        val unlockMarginX = lockCenter + cos(unlockdeg) * (lockMargin)
        val unlockMarginY = lockCenter - sin(unlockdeg) * (lockMargin)
        canvas.drawBitmap(unlockImg, Rect(0, 0, 0 + lockImg.width, 0 + lockImg.width), Rect(unlockMarginX.toInt(), unlockMarginY.toInt(), unlockMarginX.toInt() + lockWidth, unlockMarginY.toInt() + lockWidth), null)
        
    }

    fun setLock(ssm: CHSesame2) {
        if (ssm.mechSetting == null) {
            return
        }

        ssm.mechSetting?.unlockPosition
        val degree = ssm.mechStatus!!.position.toFloat()
        val lockDegree = ssm.mechSetting!!.lockPosition.toFloat()
        val unlockDegree = ssm.mechSetting!!.unlockPosition.toFloat()
        angle = degree % 360
        lockAngle = lockDegree % 360
        unlockAngle = unlockDegree % 360
        invalidate()
    }

    fun setLock(ssm: CHSesame5) {
//        L.d("hcia", "ssm.mechSetting:" + ssm.mechSetting)
//        if (ssm.mechSetting == null) {
//            return
//        }

        ssm.mechSetting?.unlockPosition
        val degree = (ssm.mechStatus?.position ?: 0).toFloat()
        val lockDegree = (ssm.mechSetting?.lockPosition ?: 0).toFloat()
        val unlockDegree = (ssm.mechSetting?.unlockPosition ?: 0).toFloat()
        angle = degree % 360
        lockAngle = lockDegree % 360
        unlockAngle = unlockDegree % 360
        invalidate()
    }


//    fun setLock(ssm: CHSesameBike2) {
////        L.d("hcia", "ssm.mechSetting:" + ssm.mechSetting)
////        if (ssm.mechSetting == null) {
////            return
////        }
//
//        ssm.mechSetting?.unlockPosition
//        val degree = (ssm.mechStatus?.position ?: 0).toFloat()
//        val lockDegree = (ssm.mechSetting?.lockPosition ?: 0).toFloat()
//        val unlockDegree = (ssm.mechSetting?.unlockPosition ?: 0).toFloat()
//        angle = degree % 360
//        lockAngle = lockDegree % 360
//        unlockAngle = unlockDegree % 360
//        invalidate()
//    }
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply {
        setRotate(-degrees - 90)
    }
    density = DisplayMetrics.DENSITY_HIGH
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Float.toDG(): Double = Math.toRadians(this.toDouble())