package co.candyhouse.app.base

import androidx.fragment.app.Fragment
import co.candyhouse.app.tabs.MainActivity

open class BaseNFG : Fragment() {
    override fun onResume() {
        super.onResume()
        (activity as MainActivity).hideMenu()
    }
}