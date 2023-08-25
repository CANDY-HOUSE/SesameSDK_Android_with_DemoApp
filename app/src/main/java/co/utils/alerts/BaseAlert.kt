package co.utils.alerts

import android.app.Dialog
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import co.candyhouse.app.R

abstract class BaseAlert(context: Context) : Dialog(context, R.style.AppTheme_FlatDialog) {

    protected lateinit var mAlertView: View

    private lateinit var mModalInAnim: Animation
    private lateinit var mModalOutAnim: Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(true)
        setCanceledOnTouchOutside(false)
        setContentView(layout)
        mAlertView = window!!.decorView.findViewById(android.R.id.content)

        mModalInAnim = AnimationUtils.loadAnimation(context, R.anim.modal_in)
        mModalOutAnim = AnimationUtils.loadAnimation(context, R.anim.modal_out)

        mModalOutAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(p0: Animation?) {}
            override fun onAnimationEnd(p0: Animation?) {
                Handler().post { super@BaseAlert.dismiss() }
            }

            override fun onAnimationStart(p0: Animation?) {}
        })

        mAlertView.viewTreeObserver.addOnGlobalLayoutListener(ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            window!!.decorView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = window!!.decorView.height
            val softHeight: Int = screenHeight - rect.bottom
//            L.d("hcia", "screenHeight:" + screenHeight)//2160
//            L.d("hcia", "softHeight:" + softHeight)
//            L.d("hcia", "rect.bottom:" + rect.bottom)
//            val scrollDistance: Int = softHeight - (screenHeight - 100)
            if (softHeight > screenHeight / 4) {
                mAlertView.scrollTo(0, screenHeight / 8)
            } else {
                mAlertView.scrollTo(0, 0)
            }
        })

    }

    protected abstract val layout: Int

    override fun onStart() {
        mAlertView.startAnimation(mModalInAnim)
    }

    override fun dismiss() {
        mAlertView.startAnimation(mModalOutAnim)
    }

}