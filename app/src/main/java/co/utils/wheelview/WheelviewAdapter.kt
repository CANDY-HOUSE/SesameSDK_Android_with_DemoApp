package co.utils.wheelview


/**
 * Created by Victor on 2017/8/8.
 */
class WheelviewAdapter(private val mList: List<String>) : IWheelviewAdapter {

    override fun getItemeTitle(i: Int): String {
        if (mList != null) {
            return mList[i]
        } else {
            return ""
        }
    }

    override val count: Int
        get() {
            if (mList != null) {
                return mList.size
            } else {
                return 0
            }

        }

    override fun get(index: Int): String {
        if (mList != null) {
            return mList[index]
        } else {
            return ""
        }
    }
}