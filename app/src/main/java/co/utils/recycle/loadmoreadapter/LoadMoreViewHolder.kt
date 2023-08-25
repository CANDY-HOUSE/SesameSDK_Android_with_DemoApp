package co.utils.recycle.loadmoreadapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import co.utils.L

internal interface ILoadMore {
    /**
     * 加载更多中
     */
    fun readty()

    /**
     * 加载更多中
     */
    fun loading()

    /**
     * 加载完成-已无更多数据
     */
    fun noMoreData()

//    /**
//     * 加载失败
//     */
//    fun loadFailed()

}

/**
 * 底部加载更多的ViewHolder
 */
internal class LoadMoreViewHolder(itemView: View, private val mFooter: ILoadMoreFooter) : RecyclerView.ViewHolder(itemView), ILoadMore {

    /**
     * 设置状态
     */
    fun setViewState(stateType: Int) {
//        L.d("hcia", "stateType:" + stateType)
        when (stateType) {
            LoadMoreAdapter.STATE_IS_LOADING -> loading()
            LoadMoreAdapter.STATE_NORMAL -> readty()
//            LoadMoreAdapter.STATE_LOAD_FAILED -> loadFailed()
            LoadMoreAdapter.STATE_NO_MORE_DATA -> noMoreData()
        }
    }

    override fun readty() {
        mFooter.ready(itemView)
    }


    override fun loading() {
        mFooter.loading(itemView)
    }

    override fun noMoreData() {
        mFooter.noMoreData(itemView)
    }

//    override fun loadFailed() {
//        mFooter.loadFailed(itemView)
//    }


}