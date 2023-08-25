package co.utils.recycle.loadmoreadapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import co.candyhouse.app.R
import co.utils.L
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent

abstract class OnSwipeListener(private val swipeThreshold: Float = 70f) : GestureDetector.SimpleOnGestureListener() {
    var checkcount = 0
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        if (distanceY > swipeThreshold) {
            checkcount++
            if (checkcount > 2) {
//                L.d("hcia", "distanceY:" + swipeThreshold)
                onSwipeUp()
            }
        } else if (distanceY < -swipeThreshold) {
            onSwipeDown()
            checkcount = 0
        }
        return super.onScroll(e1, e2, distanceX, distanceY)
    }

    abstract fun onSwipeUp()

    abstract fun onSwipeDown()
}


class LoadMoreAdapter<VH : RecyclerView.ViewHolder> private constructor(private val realAdapter: RecyclerView.Adapter<VH>, private val footer: ILoadMoreFooter = LoadMoreFooter()) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {

        private const val VIEW_TYPE_LOAD_MORE = R.layout.adapter_load_more

        const val STATE_IS_LOADING = 0//正在加载更多
        const val STATE_NORMAL = 1//正常状态
//        const val STATE_LOAD_FAILED = 2//加载失败
        const val STATE_NO_MORE_DATA = 3//没有更多数据

        @JvmStatic
        fun <VH : RecyclerView.ViewHolder> wrap(adapter: RecyclerView.Adapter<VH>, footer: ILoadMoreFooter = LoadMoreFooter()): LoadMoreAdapter<VH> {
            return LoadMoreAdapter(adapter, footer)
        }
    }

    private var sss: Boolean = false
    private var mRecyclerView: RecyclerView? = null
    private var mOnLoadMoreListener: ((adapter: LoadMoreAdapter<*>) -> Unit)? = null
    private var mStateType = STATE_NORMAL
    override fun getItemCount(): Int {
        val count = realAdapter.itemCount
        return if (count > 0) (count + 1) else 0
    }

    override fun getItemViewType(position: Int): Int {
//        if (position <= 0) return super.getItemViewType(position)
        return if (position == itemCount - 1) VIEW_TYPE_LOAD_MORE
        else realAdapter.getItemViewType(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VIEW_TYPE_LOAD_MORE) {
            return LoadMoreViewHolder(LayoutInflater.from(parent.context).inflate(viewType, parent, false), footer)
        }
        return realAdapter.onCreateViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        onBindViewHolder(holder, position, emptyList())
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>) {
        if (holder is LoadMoreViewHolder) { //如果是加载更多的VH执行onBind
            holder.setViewState(mStateType)
            return
        }
        realAdapter.onBindViewHolder(holder as VH, position, payloads)//执行正常的onBind
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        mRecyclerView = recyclerView
        val gestureDetector = GestureDetectorCompat(recyclerView.context, object : OnSwipeListener() {
            override fun onSwipeUp() {
//                Log.d("hcia", "上滑!!1")
                sss = true
            }

            override fun onSwipeDown() {
//                Log.d("hcia", "下滑!!!")
                sss = false
            }
        })
        recyclerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }
        realAdapter.registerAdapterDataObserver(mProxyDataObserver)
        recyclerView.addOnScrollListener(mOnScrollListener)
        realAdapter.onAttachedToRecyclerView(recyclerView)
    }


    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        mRecyclerView = null
        realAdapter.unregisterAdapterDataObserver(mProxyDataObserver)
        recyclerView.removeOnScrollListener(mOnScrollListener)
        realAdapter.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        if (holder is LoadMoreViewHolder) return
        realAdapter.onViewAttachedToWindow(holder as VH)
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder is LoadMoreViewHolder) return
        realAdapter.onViewDetachedFromWindow(holder as VH)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is LoadMoreViewHolder) return
        realAdapter.onViewRecycled(holder as VH)
    }

    override fun onFailedToRecycleView(holder: RecyclerView.ViewHolder): Boolean {
        return if (holder is LoadMoreViewHolder) {
            false
        } else {
            realAdapter.onFailedToRecycleView(holder as VH)
        }
    }

    /**
     * 代理原来的AdapterDataObserver
     */
    private val mProxyDataObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            this@LoadMoreAdapter.notifyDataSetChanged()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            this@LoadMoreAdapter.notifyItemRangeChanged(positionStart, itemCount)
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            this@LoadMoreAdapter.notifyItemRangeChanged(positionStart, itemCount, payload)
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            this@LoadMoreAdapter.notifyItemRangeInserted(positionStart, itemCount)
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            this@LoadMoreAdapter.notifyItemRangeRemoved(positionStart, itemCount)
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            this@LoadMoreAdapter.notifyItemRangeChanged(fromPosition, toPosition, itemCount)
        }
    }

    /**
     * 加载更多的监听
     */
    fun setOnLoadMoreListener(listener: ((adapter: LoadMoreAdapter<*>) -> Unit)? = null): LoadMoreAdapter<VH> {
        this.mOnLoadMoreListener = listener
        return this
    }

    /**
     * 滚动监听
     */


    private val mOnScrollListener = object : RecyclerView.OnScrollListener() {

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            //判断是否能加载更多
//            L.d("hcia", "newState:${newState} mStateType:${mStateType} canLoadMore:" + canLoadMore(recyclerView.layoutManager))
//            L.d("hcia", "(1):" + recyclerView.canScrollVertically(1))
//            L.d("hcia", "(-1):" + recyclerView.canScrollVertically(-1))
            if(mStateType == STATE_NO_MORE_DATA){
                return
            }
            if (canLoadMore(recyclerView.layoutManager) && mStateType != STATE_IS_LOADING && sss) {
                setState(STATE_IS_LOADING)
                sss = false
                mOnLoadMoreListener?.invoke(this@LoadMoreAdapter)
            }
        }


    }


    /**
     * 是否可以加载更多
     */
    private fun canLoadMore(layoutManager: RecyclerView.LayoutManager?): Boolean {
        return when (layoutManager) {
            is LinearLayoutManager -> {
                layoutManager.findLastVisibleItemPosition() >= layoutManager.getItemCount() - 1
            }
            else -> false
        }
    }


    /**
     * 设置底部LoadMoreViewHolder的状态
     */
    fun setState(state: Int) {
//        L.d("hcia", "mStateType:${mStateType} newstate:${state}: ")
        if (mStateType == state) return
        mStateType = state
//        L.d("hcia", "setState:" + mStateType)
        notifyLoadMoreVH()
    }


    private fun notifyLoadMoreVH() {
        if (itemCount <= 0) return
        this@LoadMoreAdapter.notifyItemChanged(itemCount - 1)
    }


}