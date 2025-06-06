package candyhouse.sesameos.ir.adapter

import android.annotation.SuppressLint
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
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.base.IrRemote
import co.candyhouse.sesame.utils.L
import java.io.Serializable


class IrCompanyAdapter(
    val context: Context,
    var children: List<IrRemote>,
    val onclick: (IrRemote,Int) -> Unit
) : RecyclerView.Adapter<IrCompanyAdapter.ChildViewHolder>() {

    var matchText = ""

    class ChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tv_name)
        val tvTag: TextView = itemView.findViewById(R.id.tv_tag)
        val lingTag:View = itemView.findViewById(R.id.line_tag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_city, parent, false)
        return ChildViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
        val childItem = children[position]
        if (matchText.isNotEmpty()) {
            val spannableString = SpannableString(childItem.alias)
            val startIndex = childItem.alias.indexOf(matchText, ignoreCase = true)
            if (startIndex >= 0) {
                spannableString.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(context, R.color.text_blue)), // 绿色
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
        holder.tvTag.visibility = View.VISIBLE
        holder.lingTag.visibility = View.VISIBLE

        if (position != 0) {
            val p = children[position - 1].direction
            if (childItem.direction == p) {
                holder.tvTag.visibility = View.GONE
                holder.lingTag.visibility = View.GONE
            }
        }

        holder.itemView.setOnClickListener {
            L.d("IRRemoteDeviceAdapter", "onItemClick: ${childItem.toString()}")
            onclick(childItem,position)
        }
    }

    override fun getItemCount(): Int = children.size

    fun updateChildren(newChildren: List<IrRemote>, matchText: String = "") {
        children = newChildren
        this.matchText = matchText
        notifyDataSetChanged()
    }
}
