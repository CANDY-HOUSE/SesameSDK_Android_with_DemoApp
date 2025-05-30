package co.candyhouse.app.tabs.devices.ssm2.setting.angle

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.ssm2.ssm5UIParser
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.LockDeviceState
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHDeviceStatus
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHSesame2
import co.candyhouse.sesame.open.device.CHSesame5
import co.candyhouse.sesame.utils.L
import kotlin.math.cos
import kotlin.math.sin

class SSMCellView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var anim: ValueAnimator? = null
    private var ssmImg: Bitmap
    private var midx: Float? = null
    private var midy: Float? = null
    private var angle: Float? = null
    private var ssmWidth: Int = 0
    private var ssmMargin: Int = 0
    private var lockWidth: Int = 0
    private var lockMargin: Int = 0
    private var lockCenter: Float = 0f
    private var dotPaint: Paint

    init {
        ssmImg = ContextCompat.getDrawable(context, R.drawable.icon_nosignal)!!.toBitmap()
        dotPaint = Paint()
        dotPaint.color = ContextCompat.getColor(context, R.color.clear)
        dotPaint.style = Paint.Style.FILL
        dotPaint.isAntiAlias = true
        dotPaint.isDither = true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        midx = width / 2.toFloat()
        midy = height / 2.toFloat()

        ssmWidth = width * 7 / 9
        ssmMargin = (width - ssmWidth) / 2

        lockWidth = width / 25
        lockMargin = ssmWidth / 2
        lockCenter = midx!! // must  x = y
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(
            ssmImg,
            Rect(0, 0, 0 + ssmImg.width, 0 + ssmImg.width),
            Rect(ssmMargin, ssmMargin, ssmMargin + ssmWidth, ssmMargin + ssmWidth),
            null
        )
        angle?.apply {
            val lockdeg: Double = this.toDG()
            val lockMarginX = lockCenter + cos(lockdeg) * (lockMargin)
            val lockMarginY = lockCenter - sin(lockdeg) * (lockMargin)
            canvas.drawCircle(
                lockMarginX.toFloat(),
                lockMarginY.toFloat(),
                lockWidth.toFloat(),
                dotPaint
            )
        }
    }

    private fun lockState(sesame: CHDevices) {
        val isBleConnect = sesame.deviceStatus.value == CHDeviceLoginStatus.Login
        val isWifiConnect = sesame.deviceShadowStatus?.value == CHDeviceLoginStatus.Login
        if (!isBleConnect && !isWifiConnect) {
            setBallColor(isConnect = false, isLock = false)
            invalidate()
            return
        }
        val deviceID = sesame.deviceId.toString()
        if (isWifiConnect) {
            setBallColor(
                isConnect = true,
                isLock = sesame.deviceShadowStatus?.name == CHDeviceStatus.Locked.name
            )
        } else {
            setBallColor(
                isConnect = true,
                isLock = sesame.deviceStatus.name == CHDeviceStatus.Locked.name
            )
        }
        L.d("lockState", "$isBleConnect    $isWifiConnect")
        if (sesame is CHSesame2) {
            angle = sesame.mechStatus?.position?.toFloat() ?: 0f
            invalidate()
            return
        }
        if (isBleConnect && isWifiConnect) {
            if (sesame.deviceStatus.name != sesame.deviceShadowStatus?.name) {
                val oldPosition = CHDeviceManager.lockStates[deviceID]?.position ?: null
                angle = oldPosition
                invalidate()
                return
            }
        }

        if (sesame is CHSesame5) {
            val oldPosition = CHDeviceManager.lockStates[deviceID]?.position ?: null
            val newPosition = sesame.mechStatus?.position?.toFloat()
            L.d("lockState", "oldPosition:${oldPosition}    $newPosition")
            if (oldPosition == null) {
                angle = newPosition
                CHDeviceManager.lockStates[deviceID] = LockDeviceState(0, newPosition)
                invalidate()
                return
            }
            newPosition?.let { newp ->
                oldPosition.let { oldp ->
                    if (newp != oldp) {
                        CHDeviceManager.lockStates[deviceID] = LockDeviceState(0, newp)
                        startAnim(oldp, newp)
                    } else {
                        angle = newp
                        invalidate()
                    }

                }
            }
        }
    }

    fun setLockImage(ssm: CHDevices) {
        ssmImg = ContextCompat.getDrawable(context, ssm5UIParser(ssm))!!.toBitmap()
        lockState(ssm)
    }

    fun setLock(sesame: CHDevices) {
        setLockImage(sesame)
    }

    private fun startAnim(old: Float, newValue: Float) {
        if (CHDeviceManager.isRefresh.get() || CHDeviceManager.isScroll.get()) {
            angle = newValue
            invalidate()
            return
        }
        anim = ValueAnimator.ofFloat(old, newValue).apply {
            duration = 500
            addUpdateListener { animation ->
                angle = animation.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    angle = newValue
                    //  CHDeviceManager.lockStates[deviceID] = LockDeviceState(0, newValue)
                    invalidate()
                }
            })
            start()
        }
    }

    private fun setBallColor(isConnect: Boolean, isLock: Boolean) {
        if (isConnect) {
            dotPaint.color = ContextCompat.getColor(
                context,
                if (isLock) R.color.lock_red else R.color.unlock_blue
            )
        } else {
            dotPaint.color = ContextCompat.getColor(context, R.color.no_grey)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        anim?.cancel()
    }
}
