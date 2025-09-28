package co.candyhouse.sesame

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFG<T : ViewBinding> : Fragment() {

    lateinit var bind: T

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            if (!::bind.isInitialized) {
                bind = getViewBinder()
            }
            bind.root
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    abstract fun getViewBinder(): T

    override fun onResume() {
        super.onResume()
    }

}