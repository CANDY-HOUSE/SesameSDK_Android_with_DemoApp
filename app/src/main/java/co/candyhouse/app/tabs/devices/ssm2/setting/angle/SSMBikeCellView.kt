package co.candyhouse.app.tabs.devices.ssm2.setting.angle

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.ssm2.ssm5UIParser
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHSesameBike
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.utils.L

class SSMBikeCellView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private var ssmImg: Bitmap

    var ssmWidth: Int = 0
    var ssmMargin: Int = 0

    init {
        ssmImg = ContextCompat.getDrawable(context, R.drawable.icon_nosignal)!!.toBitmap()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        ssmWidth = width * 7 / 9
        ssmMargin = (width - ssmWidth) / 2
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(ssmImg, Rect(0, 0, 0 + ssmImg.width, 0 + ssmImg.width), Rect(ssmMargin, ssmMargin, ssmMargin + ssmWidth, ssmMargin + ssmWidth), null)
    }

    fun setLockImage(ssm: CHDevices) {
        isVisible = (ssm !is CHSesameConnector)
        ssmImg = ContextCompat.getDrawable(context, ssm5UIParser(ssm))!!.toBitmap()
        invalidate()
        setLock(ssm)

    }

    fun setLock(ssm: CHDevices) {
        if (ssm.mechStatus == null) {
//            L.d("hcia", " mechStatus:" + ssm.mechStatus)
            return
        }
//        L.d("hcia", "${ssm.getNickname()} isStop:" + ssm.mechStatus!!.isStop)
        if (ssm.mechStatus!!.isStop == true) {
            clearAnimation()
        } else {
            startAnimation(AnimationUtils.loadAnimation(context, R.anim.shake))
        }
    }
}

