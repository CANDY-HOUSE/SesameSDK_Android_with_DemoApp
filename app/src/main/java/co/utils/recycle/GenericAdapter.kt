package co.utils.recycle

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.sesame.utils.L
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.util.Collections

abstract class GenericAdapter<T>(private var listItems: MutableList<T> = mutableListOf()) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), ItemTouchHelperAdapter {

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(listItems: MutableList<T> = mutableListOf()) {
        this.listItems = listItems
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(listItems: MutableList<T> = mutableListOf(), position: Int) {
        this.listItems = listItems
        if (position != -1) {
            notifyItemChanged(position)
        } else {
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return getViewHolder(view, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position >= 0 && position < listItems.size) {
            (holder as Binder<T>).bind(listItems[position], position)
        }
    }

    override fun getItemCount(): Int {
        return listItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return getLayoutId(position, listItems[position])
    }

    protected abstract fun getLayoutId(position: Int, obj: T): Int

    abstract fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder

    internal interface Binder<T> {
        fun bind(data: T, pos: Int)
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        L.d("GenericAdapter", "onItemMove: from $fromPosition to $toPosition")

        if (fromPosition < 0 || fromPosition >= listItems.size || toPosition < 0 || toPosition >= listItems.size) {
            return
        }
        Collections.swap(listItems, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    open fun removeItem(adapterPosition: Int) {}
}

interface ItemTouchHelperAdapter {
    fun onItemMove(fromPosition: Int, toPosition: Int)
    fun onItemMoveFinished() {}
}

class SimpleItemTouchHelperCallback(private val adapter: ItemTouchHelperAdapter) :
    ItemTouchHelper.Callback() {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = 0 // 不处理滑动事件
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        source: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        adapter.onItemMove(source.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // 不处理滑动事件
    }

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)

        try {
            // 当拖拽或滑动动作结束时，actionState 会变为 ACTION_STATE_IDLE
            // 此时 viewHolder 通常为 null，因为没有 item 被选中
            if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                // 拖拽结束，通知适配器
                adapter.onItemMoveFinished()
            }
        } catch (e: Exception) {
            L.d("sf", "SimpleItemTouchHelperCallback:onSelectedChanged Exception=${e.message}")

            FirebaseCrashlytics.getInstance().apply {
                log("SimpleItemTouchHelperCallback:onSelectedChanged Exception")
                recordException(e)
            }
        }
    }

}
