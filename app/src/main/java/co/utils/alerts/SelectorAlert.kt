package co.utils.alerts

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import co.candyhouse.app.R
import kotlinx.android.synthetic.main.alert_selector.view.*

class SelectorAlert(context: Context) : BaseAlert(context) {

    var titleText: String? = null
    var itemList = listOf<String>()
    var onItemClick: ((Int) -> Unit)? = null

    override val layout: Int get() = R.layout.alert_selector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(mAlertView) {

            titleText?.let {
                tvTitle.text = it
                tvTitle.visibility = View.VISIBLE
            }

            listview.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, itemList)

            onItemClick?.let {
                listview.setOnItemClickListener { _, _, i, _ ->
                    onItemClick!!.invoke(i)
                    this@SelectorAlert.dismiss()
                }
            }

        }

    }

}