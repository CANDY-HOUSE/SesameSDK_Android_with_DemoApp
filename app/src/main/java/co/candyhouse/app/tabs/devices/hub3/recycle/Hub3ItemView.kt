package co.candyhouse.app.tabs.devices.hub3.recycle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.base.IrRemote

class Hub3ItemView(
    private var items: List<IrRemote>,
    private val callback: (bot2: IrRemote) -> Unit
) :
    RecyclerView.Adapter<Hub3ItemView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.hub3_bot_item_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.alias
        holder.itemView.setOnClickListener {
            callback(item)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
    }

    fun updateList(listItems: ArrayList<IrRemote>) {
        this.items = listItems
        notifyDataSetChanged()
    }

}
