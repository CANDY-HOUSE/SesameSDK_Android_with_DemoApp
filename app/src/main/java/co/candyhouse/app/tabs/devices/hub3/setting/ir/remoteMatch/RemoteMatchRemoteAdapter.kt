package co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteMatch

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrMatchRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote

class RemoteMatchRemoteAdapter(
    val context: Context,
    val onclick: (IrRemote, Int) -> Unit
) : RecyclerView.Adapter<RemoteMatchRemoteAdapter.ChildViewHolder>() {
    private var dataList: MutableList<IrMatchRemote> = mutableListOf()

    class ChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_name)
        val tvMatchPercent: TextView = itemView.findViewById(R.id.tv_match_percent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fg_remote_match_code_item, parent, false)
        return ChildViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
        val childItem = dataList[position]
        holder.tvName.text = childItem.irRemote.alias
        holder.tvMatchPercent.text = childItem.matchPercent
        holder.itemView.setOnClickListener {
            onclick(childItem.irRemote, position)
        }
    }

    override fun getItemCount(): Int = dataList.size

    fun updateData(list: List<IrMatchRemote>) {
        this.dataList.clear()
        dataList.addAll(list)
        notifyDataSetChanged()
    }
}