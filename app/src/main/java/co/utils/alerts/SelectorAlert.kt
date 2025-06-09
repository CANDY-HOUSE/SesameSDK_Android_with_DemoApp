package co.utils.alerts

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import co.candyhouse.app.R

class SelectorAlert(context: Context) : BaseAlert(context) {

    var titleText: String? = null
    var itemList = listOf<String>()
    var onItemClick: ((Int) -> Unit)? = null

    override val layout: Int get() = R.layout.alert_selector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(mAlertView) {

            titleText?.let {
                findViewById<TextView>(R.id.tvTitle).text = it
                findViewById<TextView>(R.id.tvTitle).visibility = View.VISIBLE
            }

            findViewById<ListView>(R.id.listview)  .adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, itemList)

            onItemClick?.let {
                findViewById<ListView>(R.id.listview) .setOnItemClickListener { _, _, i, _ ->
                    onItemClick!!.invoke(i)
                    this@SelectorAlert.dismiss()
                }
            }

        }

    }

}