package co.candyhouse.app.tabs.devices.hub3.setting.ir.remotes

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.sesame.utils.L

class RemoteListAdapter(
    val context: Context,
    val onclick: (IrRemote, Int) -> Unit
) : RecyclerView.Adapter<RemoteListAdapter.ChildViewHolder>() {
    private var dataList: List<IrRemote> = emptyList()
    var matchText: String = ""
    private var isSearchMode: Boolean = false

    class ChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tv_name)
        val tvTag: TextView = itemView.findViewById(R.id.tv_tag)
        val lingTag: View = itemView.findViewById(R.id.line_tag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fg_remote_list_item, parent, false)
        return ChildViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
        val childItem = dataList[position]
        if (matchText.isNotEmpty()) {
            val spannableString = SpannableString(childItem.alias)
            val startIndex = childItem.alias.indexOf(matchText, ignoreCase = true)
            if (startIndex >= 0) {
                spannableString.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(context, R.color.text_blue)),
                    startIndex,
                    startIndex + matchText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            holder.nameTextView.text = spannableString
        } else {
            holder.nameTextView.text = childItem.alias
        }

        holder.tvTag.text = childItem.direction

        if (isSearchMode) {
            holder.tvTag.visibility = View.GONE
            holder.lingTag.visibility = View.GONE
        } else {
            holder.tvTag.visibility = View.VISIBLE
            holder.lingTag.visibility = View.VISIBLE

            if (position != 0) {
                val previousDirection = dataList[position - 1].direction
                if (childItem.direction == previousDirection) {
                    holder.tvTag.visibility = View.GONE
                    holder.lingTag.visibility = View.GONE
                }
            }
        }

        // 设置点击事件
        holder.itemView.setOnClickListener {
            L.d("IRRemoteDeviceAdapter", "onItemClick: $childItem")
            onclick(childItem, position)
        }
    }

    override fun getItemCount(): Int = dataList.size

    fun updateData(newData: List<IrRemote>, matchText: String = "", isSearch: Boolean = false) {
        val dataCopy = ArrayList(newData)
        this.dataList = dataCopy
        this.matchText = matchText
        this.isSearchMode = isSearch
        notifyDataSetChanged()
    }
}