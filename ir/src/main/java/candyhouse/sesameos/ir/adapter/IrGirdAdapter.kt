package candyhouse.sesameos.ir.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.base.CHHub3IRCode
import candyhouse.sesameos.ir.ext.setDebouncedClick
import candyhouse.sesameos.ir.ext.setDebouncedClickListener

class IrGridAdapter(
    private val context: Context,
    private val mData: List<CHHub3IRCode>,
    private val editable: Boolean = true,
    private val onClickItem: (CHHub3IRCode) -> Unit,
    private val onLongClickItem: (Int, CHHub3IRCode) -> Unit,
) : RecyclerView.Adapter<IrGridAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fg_ir_grid_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mData[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = mData.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val lr: View = itemView.findViewById(R.id.lr)

        fun bind(item: CHHub3IRCode) {
            tvName.text = if (item.name.isEmpty()) {
                val logClickEdit: String = context.getString(R.string.ir_long_click_edit)
                val clickSend: String = context.getString(R.string.ir_click_send)
                if (editable) {
                    (logClickEdit + "\n" + clickSend)
                } else {
                    clickSend
                }
            } else {
                item.name
            }
            itemView.setDebouncedClickListener(debounceTime = 1000,
                onDebounceEnd={
                    itemView.isEnabled = true
                    tvName.alpha = 1f
                },
                action = {
                    itemView.isEnabled = false
                    tvName.alpha = 0.1f
                    onClickItem(item)
                })
            itemView.setOnLongClickListener {
                onLongClickItem(adapterPosition,item)
                true
            }
            lr.visibility = if (adapterPosition % 3 == 2) View.GONE else View.VISIBLE
        }
    }
}
