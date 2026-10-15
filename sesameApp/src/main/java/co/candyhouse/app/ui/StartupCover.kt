package co.candyhouse.app.ui

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import co.candyhouse.app.R
import co.candyhouse.app.util.dp

/** Covers only the initial document. The WebView remains visible and continues drawing underneath. */
class StartupCover(activity: Activity, private val parent: FrameLayout, retry: () -> Unit) {
    var active = true
        private set
    private var generation = 0L
    private var removeSystemSplash: (() -> Unit)? = null
    private val coverIcon = ImageView(activity).apply {
        setImageResource(R.mipmap.ic_launcher_round)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private val cover = FrameLayout(activity).apply {
        val colors = activity.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
        setBackgroundColor(colors.getColor(0, android.graphics.Color.WHITE))
        colors.recycle()
        isClickable = true
        addView(coverIcon, FrameLayout.LayoutParams(activity.dp(192), activity.dp(192), Gravity.CENTER))
    }
    private val retryButton = ImageButton(activity).apply {
        setImageResource(android.R.drawable.ic_popup_sync)
        imageTintList = ColorStateList.valueOf(0xff28aeb1.toInt())
        val padding = activity.dp(14)
        setPadding(padding, padding, padding, padding)
        val attrs = activity.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackgroundBorderless))
        background = attrs.getDrawable(0)
        attrs.recycle()
        visibility = View.GONE
        setOnClickListener {
            if (active) {
                begin()
                retry()
            }
        }
    }
    private val timeout = Runnable {
        showRetry()
    }

    init {
        cover.addView(retryButton, FrameLayout.LayoutParams(activity.dp(56), activity.dp(56), Gravity.CENTER).apply {
            topMargin = activity.dp(240)
        })
        parent.addView(cover, FrameLayout.LayoutParams(-1, -1))
        if (Build.VERSION.SDK_INT >= 31) {
            // Retain the existing system artwork without blocking the Activity's first draw.
            activity.splashScreen.setOnExitAnimationListener { splash ->
                // Preserve the system's mask, padding and placement when the fallback is exposed.
                val icon = splash.iconView
                if (active && icon != null && icon.width > 0 && icon.height > 0) {
                    val bitmap = Bitmap.createBitmap(icon.width, icon.height, Bitmap.Config.ARGB_8888)
                    icon.draw(Canvas(bitmap))
                    val iconLocation = IntArray(2)
                    val coverLocation = IntArray(2)
                    icon.getLocationOnScreen(iconLocation)
                    cover.getLocationOnScreen(coverLocation)
                    coverIcon.setImageBitmap(bitmap)
                    coverIcon.layoutParams = FrameLayout.LayoutParams(icon.width, icon.height, Gravity.TOP or Gravity.LEFT).apply {
                        leftMargin = iconLocation[0] - coverLocation[0]
                        topMargin = iconLocation[1] - coverLocation[1]
                    }
                }
                if (!active || retryButton.visibility == View.VISIBLE) {
                    splash.remove()
                } else removeSystemSplash = {
                    splash.remove()
                }
            }
        }
        begin()
    }

    fun begin() {
        if (!active) return
        generation++
        retryButton.visibility = View.GONE
        cover.removeCallbacks(timeout)
        // A slow page may still finish after this; expose retry without cancelling its load.
        cover.postDelayed(timeout, 8000)
    }

    fun ready(web: WebView) {
        if (!active) return
        val expected = generation
        web.postVisualStateCallback(expected, object : WebView.VisualStateCallback() {
            override fun onComplete(requestId: Long) {
                if (active && requestId == generation) close()
            }
        })
    }

    fun fail() {
        if (!active) return
        generation++
        // Offline fallback may recover immediately. Keep the existing deadline and artwork.
    }

    private fun showRetry() {
        if (!active) return
        cover.removeCallbacks(timeout)
        retryButton.visibility = View.VISIBLE
        removeSystemSplash?.invoke()
        removeSystemSplash = null
    }

    fun close() {
        active = false
        generation++
        cover.removeCallbacks(timeout)
        parent.removeView(cover)
        removeSystemSplash?.invoke()
        removeSystemSplash = null
    }
}
