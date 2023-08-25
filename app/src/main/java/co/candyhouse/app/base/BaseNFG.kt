package co.candyhouse.app.base

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import co.candyhouse.sesame.utils.L
import kotlinx.android.synthetic.main.back_sub.*

open class BaseNFG(layout: Int) : Fragment(layout) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // back_zone: 返回紐
        back_zone?.setOnClickListener {
            // Nav返回前一層
            findNavController().navigateUp()
        }
    }

    override fun onResume() {
        super.onResume()
        L.l("fragment",this::class.java.simpleName)
    }
}