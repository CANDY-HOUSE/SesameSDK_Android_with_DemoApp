
package co.candyhouse.app.tabs.menu

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import co.candyhouse.app.R


data class BarMenuItem(
        val index: Int,
        val icon: Drawable?,
        val title: String
)

object ItemUtils {


    fun getCustomSamples(context: Context): List<BarMenuItem> {
        val samples = ArrayList<BarMenuItem>()
        samples.add(BarMenuItem(1, drawable(context, R.drawable.ic_cube_2), context.getString(R.string.new_sesame)))
      //  samples.add(BarMenuItem(2, drawable(context, R.drawable.ic_qr_code_scan_2), context.getString(R.string.scan_the_qr_code)))//todo k ic_icons_filled_scan

        return samples
    }

    private fun drawable(context: Context, @DrawableRes id: Int): Drawable? {
        return ContextCompat.getDrawable(context, id)
    }
}