package co.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Movie
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import java.io.InputStream

class GifMovieView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var movie: Movie? = null
    private var movieStart: Long = 0L



    fun initView(gifView:Int){
        val inputStream: InputStream = context.resources.openRawResource(gifView)
        movie = Movie.decodeStream(inputStream)
        movieStart=0L
        invalidate()

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (movie == null) {
            return
        }

        val now = SystemClock.uptimeMillis()
        if (movieStart == 0L) {
            movieStart = now
        }

        movie?.let {
            val duration = if (it.duration() == 0) 1000 else it.duration()
            val relTime = ((now - movieStart) % duration).toInt()
            it.setTime(relTime)

            val scaleWidth = width.toFloat() / it.width()
            val scaleHeight = height.toFloat() / it.height()
            val scale = Math.min(scaleWidth, scaleHeight) // Maintain aspect ratio

            val dx = (width - it.width() * scale) / 2f
            val dy = (height - it.height() * scale) / 2f

            canvas?.save()
            canvas?.scale(scale, scale)
            canvas?.translate(dx / scale, dy / scale)
            it.draw(canvas, 0f, 0f)
            canvas?.restore()

            invalidate()
        }
    }
}