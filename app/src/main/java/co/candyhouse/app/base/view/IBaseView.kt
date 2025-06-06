package co.candyhouse.app.base.view

import android.view.View

/**
 * UI基本视图
 *
 * @author frey on 2025/3/31
 */
interface IBaseView {

    fun setupUI()

    fun setupListeners()

    fun <T : View> observeViewModelData(view: T)
}