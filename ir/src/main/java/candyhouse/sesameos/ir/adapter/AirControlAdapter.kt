package candyhouse.sesameos.ir.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import candyhouse.sesameos.ir.databinding.ItemAirControlBinding
import candyhouse.sesameos.ir.models.IrControlItem
import co.candyhouse.sesame.utils.L

class AirControlViewsAdapter(
    private val onItemClick: (IrControlItem) -> Unit
) : ListAdapter<IrControlItem, AirControlViewsAdapter.AirControlViewHolder>(AirControlDiffCallback()) {
    private var currentPosition = -1
    private val handleDelay = 800L
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AirControlViewHolder {
        return AirControlViewHolder.create(parent, onItemClick, this)
    }

    override fun onBindViewHolder(holder: AirControlViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }


    class AirControlViewHolder private constructor(
        private val binding: ItemAirControlBinding,
        private val onItemClick: (IrControlItem) -> Unit,
        private val adapter: AirControlViewsAdapter
    ) : RecyclerView.ViewHolder(binding.root) {
        private val columnsPerRow = 3
        fun bind(item: IrControlItem, position: Int) {
            binding.apply {
                tvTitle.setText(item.title)
                rightLine.visibility = if (isLastColumn(adapterPosition)) View.GONE else View.VISIBLE
                bottomLine.visibility = if (isInLastRow(adapterPosition)) View.GONE else View.VISIBLE
                if (item.iconRes == 0) {
                    ivIcon.setImageDrawable(null)
                } else {
                    ivIcon.setImageResource(item.iconRes)
                }
                if (adapter.currentPosition == position) {
                    tvTitle.alpha = 0.1f
                } else {
                    tvTitle.alpha = 1f
                }
//                root.isSelected = item.isSelected
                root.setOnClickListener({
                    if (adapter.currentPosition == -1) {
                        adapter.currentPosition = position
                        adapter.notifyDataSetChanged()
                        onItemClick(item)
                        root.postDelayed({
                            adapter.currentPosition = -1
                            adapter.notifyDataSetChanged()
                        }, adapter.handleDelay)
                    }
                })


            }
        }

        private fun isInLastRow(position: Int): Boolean {
            val totalRows = (adapter.itemCount + columnsPerRow - 1) / columnsPerRow
            val currentRow = position / columnsPerRow
            return currentRow == totalRows - 1
        }

        private fun isLastColumn(position: Int): Boolean {
            return position % columnsPerRow == (columnsPerRow - 1)
        }

        companion object {
            fun create(
                parent: ViewGroup,
                onItemClick: (IrControlItem) -> Unit,
                adapter: AirControlViewsAdapter
            ): AirControlViewHolder {
                val binding = ItemAirControlBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return AirControlViewHolder(binding, onItemClick, adapter)
            }
        }
    }
}


class AirControlDiffCallback : DiffUtil.ItemCallback<IrControlItem>() {
    override fun areItemsTheSame(oldItem: IrControlItem, newItem: IrControlItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: IrControlItem, newItem: IrControlItem): Boolean {
        return oldItem == newItem
    }
}