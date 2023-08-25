package co.utils.recycle.loadmoreadapter

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import co.candyhouse.app.R
import co.utils.L

interface ILoadMoreFooter {


    /**
     * 準備加載更多
     */
    fun ready(footerView: View)

    /**
     * 加载更多中
     */
    fun loading(footerView: View)

    /**
     * 加载完成-已无更多数据
     */
    fun noMoreData(footerView: View)

//    /**
//     * 加载失败
//     */
//    fun loadFailed(footerView: View)
}

/**
 * 默认的底部加载布局
 */
internal class LoadMoreFooter : ILoadMoreFooter {

    override fun ready(footerView: View) {
//        L.d("hcia", "ready:" )
        footerView.findViewById<ProgressBar>(R.id.load_more_pb)?.apply {
            visibility = View.GONE
        }
        footerView.findViewById<TextView>(R.id.load_more_tv)?.apply {
            visibility = View.VISIBLE
            text = "pull_up_to_load_more"
        }
    }

    override fun loading(footerView: View) {
//        L.d("hcia", "loading:" )
        footerView.findViewById<ProgressBar>(R.id.load_more_pb)?.apply {
            visibility = View.VISIBLE
        }
        footerView.findViewById<TextView>(R.id.load_more_tv)?.apply {
            visibility = View.GONE
//            text = "loading"
        }

    }

    override fun noMoreData(footerView: View) {
        footerView.findViewById<ProgressBar>(R.id.load_more_pb)?.apply {
            visibility = View.GONE
        }

        footerView.findViewById<TextView>(R.id.load_more_tv)?.apply {
            visibility = View.VISIBLE
            text = footerView.context.getString(R.string.no_more)
        }
    }

//    override fun loadFailed(footerView: View) {
//        L.d("hcia", "loading:")
//
//        footerView.findViewById<ProgressBar>(R.id.load_more_pb)?.apply {
//            visibility = View.VISIBLE
//        }
//        footerView.findViewById<TextView>(R.id.load_more_tv)?.apply {
//            visibility = View.VISIBLE
//            text = footerView.context.getString(R.string.load_fail)
//        }
//    }
}