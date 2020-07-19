package co.utils.wheelview

/**
 * Created by Victor on 2017/8/1.
 */

interface IWheelviewAdapter {
    fun getItemeTitle(i: Int): kotlin.String
    val count: Int
    operator fun get(index: Int): Any
}
