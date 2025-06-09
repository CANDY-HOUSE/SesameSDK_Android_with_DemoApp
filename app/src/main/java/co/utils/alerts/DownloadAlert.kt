package co.utils.alerts

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import co.candyhouse.app.R


class DownloadAlert(context: Context) : BaseAlert(context) {

    var titleText: String? = null

    override val layout: Int get() = R.layout.alert_download

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(mAlertView) {

            titleText?.let {

                findViewById<TextView>(R.id.tvTitle).text = it
                findViewById<TextView>(R.id.tvTitle).visibility = View.VISIBLE
            }



            findViewById<ProgressBar>(R.id.progressBar).isIndeterminate = true


            val set = AnimatorInflater.loadAnimator(context, R.animator.cloud_dowload) as AnimatorSet
            set.setTarget(findViewById<ImageView>(R.id.ivArrow))
            set.start()

        }

    }

    fun updateProgress(progress: Int, total: Int) {
        with(mAlertView) {
            findViewById<ProgressBar>(R.id.progressBar).isIndeterminate = false
            findViewById<ProgressBar>(R.id.progressBar).progress = progress
            findViewById<ProgressBar>(R.id.progressBar).max = total

            findViewById<TextView>(R.id.tvPercentProgress).text = "${(progress * 100) / total}%"
            findViewById<TextView>(R.id.tvStatusProgress)   .text = "$progress/$total"
        }
    }

    private fun reset() {
        with(mAlertView) {
            findViewById<ProgressBar>(R.id.progressBar).isIndeterminate = true
            findViewById<ProgressBar>(R.id.progressBar).progress = 0
            findViewById<ProgressBar>(R.id.progressBar).max = 0
            findViewById<TextView>(R.id.tvPercentProgress).text = ""
            findViewById<TextView>(R.id.tvStatusProgress).text = ""
        }
    }

    override fun cancel() {
        reset()
        super.cancel()
    }

    override fun dismiss() {
        reset()
        super.dismiss()
    }

}