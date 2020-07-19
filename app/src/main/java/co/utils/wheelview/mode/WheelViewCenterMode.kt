package com.victor.library.wheelview.mode

import android.graphics.Paint

/**
 * Created by Victor on 2017/7/20.
 */
class WheelViewCenterMode(override var eachItemHeight: Int,
                          override var childrenSize: Int) : IWheelViewMode {

    override fun getSelectedIndex(baseIndex: Int): Int {
        return baseIndex + childrenSize / 2
    }

    override fun getTopMaxScrollHeight(): Int {
        return -eachItemHeight * (childrenSize - 1) / 2
    }

    override fun getBottomMaxScrollHeight(): Int {
        return eachItemHeight * (childrenSize - 1) / 2
    }

    override fun getTextDrawY(height: Int, index: Int, paint: Paint): Float {
        return (getCenterY(height, paint) + (index - (childrenSize - 1) / 2) * eachItemHeight)
    }
}