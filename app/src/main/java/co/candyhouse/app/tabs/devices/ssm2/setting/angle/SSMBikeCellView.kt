package co.candyhouse.app.tabs.devices.ssm2.setting.angle

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.app.tabs.devices.ssm2.ssm5UIParser
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.open.device.CHSesameConnector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SSMBikeCellView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private var ssmImg: Bitmap

    private var ssmWidth: Int = 0
    private var ssmMargin: Int = 0
    private var animationJob: Job? = null

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
        stopAnimationAndThread()
        isVisible =
            if (ssm !is CHSesameConnector) true else ssm.productModel == CHProductModel.SSMOpenSensor && ssm.mechStatus != null
        ssmImg = ContextCompat.getDrawable(context, ssm5UIParser(ssm))!!.toBitmap()
        invalidate()
        setLock(ssm)

    }

    private fun startAnimationTimer(timeInSeconds: Int) {
        animationJob = CoroutineScope(Dispatchers.Main).launch {
            delay(timeInSeconds * 1000L)
            if (animation != null && animation.hasStarted() && !animation.hasEnded()) {
                clearAnimation()

            }
        }
    }

    private fun stopAnimationAndThread() {
        animationJob?.cancel() // 停止监控线程
        clearAnimation() // 停止动画
    }
    private fun setLock(ssm: CHDevices) {
        if (ssm.mechStatus == null) {
            return
        }


        if (ssm.mechStatus!!.isStop == true) {
            clearAnimation()
        } else {
            clearAnimation()
          //  startAnimation(AnimationUtils.loadAnimation(context, R.anim.shake))
            startAnimation(shakeAnim())
            startAnimationTimer(3)
        }
    }
    private fun shakeAnim():TranslateAnimation{
      val  shakeAnimation = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, -0.03f,  // fromXDelta
                Animation.RELATIVE_TO_SELF, 0.03f,  // toXDelta
                Animation.RELATIVE_TO_SELF, 0f,  // fromYDelta (保持Y轴不动)
                Animation.RELATIVE_TO_SELF, 0f) // toYDelta (保持Y轴不动)
        shakeAnimation.duration = 60
        shakeAnimation.interpolator = LinearInterpolator()
        shakeAnimation.repeatCount = Animation.INFINITE
        shakeAnimation.repeatMode = Animation.REVERSE
        return  shakeAnimation
    }
}

