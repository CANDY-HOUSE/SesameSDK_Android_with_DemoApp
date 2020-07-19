package com.victor.library.wheelview.mode

import android.graphics.Paint

/**
 * Created by Victor on 2017/6/28.
 */
interface IWheelViewMode {
    var eachItemHeight : Int
    var childrenSize : Int

    fun getSelectedIndex(baseIndex : Int): Int
    fun getTopMaxScrollHeight(): Int
    fun getBottomMaxScrollHeight(): Int
    fun getTextDrawY(height: Int, index : Int, paint: Paint): Float

    fun getCenterY(height : Int, paint: Paint) : Float {
        return (height - paint.fontMetricsInt.bottom - paint.fontMetricsInt.top) / 2.toFloat()
    }
}







