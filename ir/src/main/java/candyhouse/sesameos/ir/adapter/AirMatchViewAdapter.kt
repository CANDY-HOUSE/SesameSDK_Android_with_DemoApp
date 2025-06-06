package candyhouse.sesameos.ir.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.databinding.ItemAirControlBinding
import candyhouse.sesameos.ir.models.IrControlItem

class AirMatchViewAdapter(
    private val context: Context,
    private val onItemClick: (IrControlItem, Int) -> Unit
) : ListAdapter<IrControlItem, AirMatchViewAdapter.AirMatchViewHolder>(AirMatchDiffCallback()) {
    var currentPosition = 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AirMatchViewHolder {
        return AirMatchViewHolder.create(context, parent, onItemClick, this)
    }

    override fun onBindViewHolder(holder: AirMatchViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }


    class AirMatchViewHolder private constructor(
        private val context: Context,
        private val binding: ItemAirControlBinding,
        private val onItemClick: (IrControlItem, Int) -> Unit,
        private val adapter: AirMatchViewAdapter
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: IrControlItem, position: Int) {
            binding.apply {
                tvTitle.setText(item.title)
                if (item.iconRes == 0) {
                    ivIcon.setImageDrawable(null)
                } else {
                    ivIcon.setImageResource(item.iconRes)
                }
                if (adapter.currentPosition == position) {
                    itemContainer.setBackgroundResource(R.drawable.bg_match_working)
                    tvTitle.setTextColor(ContextCompat.getColor(context, R.color.white))
                } else {
                    itemContainer.setBackgroundResource(R.drawable.bg_item)
                    tvTitle.setTextColor(ContextCompat.getColor(context, R.color.black))
                }
                if (position < adapter.currentPosition) {
                    val drawable = ContextCompat.getDrawable(context, R.drawable.icon_match_success)
                    drawable?.setBounds(0, 0, 60, 60)
                    tvTitle.setCompoundDrawables(drawable, null, null, null)
                } else {
                    tvTitle.setCompoundDrawables(null, null, null, null)
                }
                root.setOnClickListener({
                    if (position == adapter.currentPosition) {
                        itemContainer.isEnabled = false
                        root.postDelayed({
                            itemContainer.isEnabled = true
                        }, 1200)
                        onItemClick(item,position)
                    } else {
                        Toast.makeText(context,R.string.air_match_item_tips,Toast.LENGTH_SHORT).show()
                    }
                })


            }
        }

        companion object {
            fun create(
                context: Context,
                parent: ViewGroup,
                onItemClick: (IrControlItem, Int) -> Unit,
                adapter: AirMatchViewAdapter
            ): AirMatchViewHolder {
                val binding = ItemAirControlBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return AirMatchViewHolder(context, binding, onItemClick, adapter)
            }
        }
    }
}


class AirMatchDiffCallback : DiffUtil.ItemCallback<IrControlItem>() {
    override fun areItemsTheSame(oldItem: IrControlItem, newItem: IrControlItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: IrControlItem, newItem: IrControlItem): Boolean {
        return oldItem == newItem
    }
}