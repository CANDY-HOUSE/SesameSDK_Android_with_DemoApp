//package co.utils.recycle
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.recyclerview.widget.ItemTouchHelper
//import androidx.recyclerview.widget.RecyclerView
//import co.utils.L
//import java.util.*
//
//
//abstract class GenericAdapter<T> : RecyclerView.Adapter<RecyclerView.ViewHolder>, ItemTouchHelperAdapter {
//
//    var listItems: List<T>
//
//    constructor(listItems: List<T>) {
//        this.listItems = listItems
//    }
//
//    constructor() {
//        listItems = emptyList()
//    }
//
////    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
////        super.onViewAttachedToWindow(holder)
////        val position = holder.adapterPosition
//////        L.d("hcia", "position++:" + position)
////    }
//
////    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
////        super.onViewDetachedFromWindow(holder)
////        val position = holder.adapterPosition
//////        L.d("hcia", "position--:" + position)
////    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
//        return getViewHolder(view, viewType)
//    }
//
//    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//        (holder as Binder<T>).bind(listItems[position], position)
//    }
//
//    override fun getItemCount(): Int {
//        return listItems.size
//    }
//
//    override fun getItemViewType(position: Int): Int {
//        return getLayoutId(position, listItems[position])
//    }
//
//    protected abstract fun getLayoutId(position: Int, obj: T): Int
//
//    abstract fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder
//
//    internal interface Binder<T> {
//        fun bind(data: T, pos: Int)
//    }
//
//
//    override fun onItemMove(fromPosition: Int, toPosition: Int) {
//        Collections.swap(listItems, fromPosition, toPosition)
//        notifyItemMoved(fromPosition, toPosition)
//    }
//
//}
//
//interface ItemTouchHelperAdapter {
//    fun onItemMove(fromPosition: Int, toPosition: Int)
//    fun onItemMoveFinished() {}
//
//}
//
//class SimpleItemTouchHelperCallback(private val adapter: ItemTouchHelperAdapter) : ItemTouchHelper.Callback() {
//
//    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
//        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
//        val swipeFlags = 0 // 不处理滑动事件
//        return makeMovementFlags(dragFlags, swipeFlags)
//    }
//
//    override fun onMove(recyclerView: RecyclerView, source: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
//        adapter.onItemMove(source.adapterPosition, target.adapterPosition)
//        return true
//    }
//
//
//    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
//        // 不处理滑动事件
//    }
//
//    override fun isLongPressDragEnabled(): Boolean {
//        return true
//    }
//
//    override fun isItemViewSwipeEnabled(): Boolean {
//        return true
//    }
//
//    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
//        super.clearView(recyclerView, viewHolder)
////        L.d("hcia", "clearView!!!:" )
//        adapter.onItemMoveFinished()
//    }
//
//
//}