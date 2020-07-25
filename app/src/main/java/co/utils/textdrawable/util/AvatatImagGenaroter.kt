package co.utils.textdrawable.util

import co.utils.textdrawable.TextDrawable


fun avatatImagGenaroter(name: String? = "NA"): TextDrawable? {
    //todo cut

    val na = name ?: "NA"
    val ts = if (na.length == 1) 60 else 38
    val drawable = TextDrawable.Builder()
            .setColor(ColorGenerator.DEFAULT.getColorByIndex(na.firstOrNull()?.toInt()))
            .setShape(TextDrawable.SHAPE_ROUND)
            .setText(na)
            .setFontSize(ts)
            .build()
    return drawable
}