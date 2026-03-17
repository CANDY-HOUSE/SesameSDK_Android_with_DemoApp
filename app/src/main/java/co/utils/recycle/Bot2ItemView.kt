package co.utils.recycle

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import java.util.Collections

data class BotItem(val name: String, val id: Int, var displayOrder: Int = Int.MAX_VALUE)
class Bot2ItemView(
    private val list: MutableList<BotItem>,
    val callback: (bot2: BotItem) -> Unit,
    val onOrderChanged: ((list: List<BotItem>) -> Unit)? = null
) : RecyclerView.Adapter<Bot2ItemView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adp_bot_item_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvName.text = item.name
        holder.itemView.setOnClickListener {
            callback(item)
        }
    }

    override fun getItemCount(): Int = list.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<BotItem>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) return
        if (fromPosition !in list.indices || toPosition !in list.indices) return
        if (fromPosition == toPosition) return

        Collections.swap(list, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun onDragFinished() {
        list.forEachIndexed { index, item ->
            item.displayOrder = index
        }
        onOrderChanged?.invoke(list.toList())
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }
}