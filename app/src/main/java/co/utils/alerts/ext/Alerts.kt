package pe.startapps.alerts.ext

import android.content.Context
import androidx.fragment.app.Fragment
import pe.startapps.alerts.CheckAlert
import pe.startapps.alerts.DownloadAlert
import pe.startapps.alerts.SelectorAlert
import pe.startapps.alerts.StandardAlert


fun Context.alert(title: String? = null,content: String? = null, init: (CheckAlert.() -> Unit)? = null) = CheckAlert(this).apply {
    this.titleText = title
    this.contentText = content
    if (init != null) init()
}

fun Context.progressAlert(title: String? = null,  init: (StandardAlert.() -> Unit)? = null) = StandardAlert(this, AlertType.Progress).apply {
    this.titleText = title
//    this.contentText = content
    if (init != null) init()
}

fun Context.inputTextAlert(tophint: String? = null,title: String? = null, hint: String?, inputType: InputType = InputType.Text, init: (StandardAlert.() -> Unit)? = null) = StandardAlert(this, AlertType.InputText).apply {
    this.titleText = title
    this.lastName = hint
    this.inputType = inputType
    this.topHinText = tophint
    if (init != null) init()
}

fun Context.inputNameAlert(title: String? = null, lastName: String?, firstName: String?, inputType: InputType = InputType.Text, init: (StandardAlert.() -> Unit)? = null) = StandardAlert(this, AlertType.TwoEdit).apply {
    this.titleText = title
    this.lastName = lastName
    this.firstName = firstName
    this.inputType = inputType
    if (init != null) init()
}

fun Context.selectorAlert(title: String? = null, itemList: List<String>, onItemClick: (Int) -> Unit) = SelectorAlert(this).apply {
    this.titleText = title
    this.itemList = itemList
    this.onItemClick = onItemClick
}

fun Context.downloadAlert(title: String? = null, init: (DownloadAlert.() -> Unit)? = null) = DownloadAlert(this).apply {
    this.titleText = title
    if (init != null) init()
}

fun Fragment.Alert(title: String, content: String, init: (CheckAlert.() -> Unit)? = null) = context?.alert(title,content, init)

fun Fragment.InputTextAlert(tophint: String? = null,title: String? = null, hint: String, inputType: InputType = InputType.Text, init: (StandardAlert.() -> Unit)? = null) = context?.inputTextAlert(tophint,title, hint, inputType, init)

fun Fragment.SelectorAlert(title: String? = null, itemList: List<String>, onItemClick: (Int) -> Unit) = context?.selectorAlert(title, itemList, onItemClick)

fun Fragment.DownloadAlert(title: String? = null, content: String? = null) = context?.downloadAlert(title)

enum class AlertType {

    Normal,
    Warning,
    TwoEdit,
    InputText,
    Progress

}

enum class InputType(val value: Int) {

    Number(2),
    Text(1)

}