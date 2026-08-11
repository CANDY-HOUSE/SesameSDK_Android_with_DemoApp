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
import co.candyhouse.sesame.open.devices.CHSesame2
import co.candyhouse.sesame.open.devices.CHSesame5
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
    // 切换点标记：上锁与解锁图标各缩小到一半，并列显示在角度轨道对应位置
    private var switchPointLockImg: Bitmap =
        ContextCompat.getDrawable(context, R.drawable.ic_icon_lock_uncheck)!!.toBitmap()
    private var switchPointUnlockImg: Bitmap =
        ContextCompat.getDrawable(context, R.drawable.ic_icon_unlock_uncheck)!!.toBitmap()

    private var midx = 0f
    private var midy = 0f

    private var angle = 0f
    private var lockAngle = 0f
    private var unlockAngle = 0f
    // 是否在角度轨道上绘制切换点标记（固件上报过切换点能力时由 setSwitchPoint 置 true，clearSwitchPoint 置 false）。
    private var hasLockUnlockSwitchPointSetting = false
    // 开锁/上锁切换点角度（度）。
    private var switchPointAngle = 45

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
        if (hasLockUnlockSwitchPointSetting) {
            // 切换点标记：上锁/解锁图标各缩小一半，沿轨道切向方向并列显示在该角度位置。
            // 切向方向 = (sin S, cos S)；沿轨道为“角度减小/顺时针”方向（与角度增大方向相反）。
            val halfSize = iconSize / 2
            val rad = Math.toRadians(switchPointAngle.toDouble())
            val tx = sin(rad).toFloat()
            val ty = cos(rad).toFloat()
            val d = halfSize / 2f
            val (forwardImg, backwardImg) = switchPointSidePair()
            drawIcon(canvas, forwardImg, switchPointAngle.toFloat(), halfSize, offsetX = tx * d, offsetY = ty * d)
            drawIcon(canvas, backwardImg, switchPointAngle.toFloat(), halfSize, offsetX = -tx * d, offsetY = -ty * d)
        }
    }

    /** 根据切换点相对于 lock/unlock 角度的位置，返回 [切向正向图标, 切向负向图标]。
     *  切向正向(+offset)方向为 (sin S, cos S)，沿轨道是“角度减小/顺时针”的方向；
     *  切向负向(-offset)为“角度增大/逆时针”的方向。
     *  因此：若某角度从 S 顺时针(角度减小)可达，则其图标应画在 +offset 侧，即 norm360(A - S) > 180。 */
    private fun switchPointSidePair(): Pair<Bitmap, Bitmap> {
        val s = norm360(switchPointAngle.toFloat())
        val la = norm360(lockAngle)
        val ua = norm360(unlockAngle)
        // 某角度落在 +offset(顺时针/角度减小)半区，当 norm360(A - S) ∈ (180, 360)。
        val lockOnForward = norm360(la - s) > 180f
        val unlockOnForward = norm360(ua - s) > 180f
        // lock / unlock 应分处切换点两侧（lock 与 unlock 角度一般分居切换点两边）。
        return if (lockOnForward == unlockOnForward) {
            // 极端情况：两者落在同一侧，维持默认（lock 正向、unlock 负向）。
            switchPointLockImg to switchPointUnlockImg
        } else if (lockOnForward) {
            switchPointLockImg to switchPointUnlockImg
        } else {
            switchPointUnlockImg to switchPointLockImg
        }
    }

    private fun norm360(deg: Float): Float {
        var d = deg % 360f
        if (d < 0f) d += 360f
        return d
    }

    private fun drawIcon(canvas: Canvas, bmp: Bitmap, degFloat: Float, size: Int, offsetX: Float = 0f, offsetY: Float = 0f) {
        val rad = Math.toRadians(degFloat.toDouble())
        val cx = midx + cos(rad) * orbitRadius + offsetX
        val cy = midy - sin(rad) * orbitRadius + offsetY

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

    /** 设置开锁/上锁切换点角度（度），并在角度视图上标记。 */
    fun setSwitchPoint(angleDeg: Int) {
        post {
            switchPointAngle = ((angleDeg % 360) + 360) % 360
            hasLockUnlockSwitchPointSetting = true
            invalidate()
        }
    }

    /** 清除切换点标记（固件不支持该能力时调用）。 */
    fun clearSwitchPoint() {
        post {
            hasLockUnlockSwitchPointSetting = false
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