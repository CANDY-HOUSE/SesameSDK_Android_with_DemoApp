package co.utils.wheelview


/**
 * Created by Victor on 2017/8/8.
 */
class WheelviewAdapter(private val mList: List<String>) : IWheelviewAdapter {

    override fun getItemeTitle(i: Int): String {
        return mList[i]
    }

    override val count: Int
        get() {
            return mList.size
        }

    override fun get(index: Int): String {
        return mList[index]
    }
}