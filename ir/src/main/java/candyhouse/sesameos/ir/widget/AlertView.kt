package candyhouse.sesameos.ir.widget

import androidx.appcompat.app.AppCompatActivity
import candyhouse.sesameos.ir.widget.dialog.AlertAction
import candyhouse.sesameos.ir.widget.dialog.BottomSheetFragment

enum class AlertStyle {
    BOTTOM_SHEET,

    IOS
}
enum class AlertTheme {
    LIGHT,
    DARK
}
/**
 * Created by hammad.akram on 3/14/18.
 */
class AlertView(private var title: String, private var message: String, private var style: AlertStyle) {

    private var theme: AlertTheme = AlertTheme.LIGHT
    private var actions: ArrayList<AlertAction> = ArrayList()

    /**
     * Add Actions to AlertView
     * @param action: AlertAction
     */
    fun addAction(action: AlertAction) {
        actions.add(action)
    }

    /**
     * Show AlertView
     * @param activity: AppCompatActivity
     */
    fun show(activity: AppCompatActivity) {
        when (style) {
            AlertStyle.BOTTOM_SHEET -> {
                val bottomSheet = BottomSheetFragment(title, message, actions, style, theme)
                bottomSheet.show(activity.supportFragmentManager, bottomSheet.tag)
            }
            AlertStyle.IOS -> {
                val bottomSheet = BottomSheetFragment(title, message, actions, style, theme)
                bottomSheet.show(activity.supportFragmentManager, bottomSheet.tag)
            }
        /*    AlertStyle.DIALOG -> {
                val bottomSheet = DialogFragment(title, message, actions, theme)
                bottomSheet.show(activity.supportFragmentManager, bottomSheet.tag)
            }*/
        }
    }

    /**
     * Set theme for the AlertView
     * @param theme: AlertTheme
     */
    fun setTheme(theme: AlertTheme) {
        this.theme = theme
    }
}