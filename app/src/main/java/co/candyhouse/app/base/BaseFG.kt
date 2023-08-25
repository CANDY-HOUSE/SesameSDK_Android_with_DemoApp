package co.candyhouse.app.base

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import co.candyhouse.app.R
import co.candyhouse.app.tabs.menu.BarMenuItem
import co.candyhouse.app.tabs.menu.CustomAdapter
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import co.candyhouse.app.tabs.menu.ItemUtils
import co.utils.L
import com.skydoves.balloon.*

open class BaseFG(layout: Int) : Fragment(layout) {
    lateinit var customListBalloon: Balloon
    private val customAdapter by lazy {
        CustomAdapter(object : CustomAdapter.CustomViewHolder.Delegate {
            override fun onCustomItemClick(customItem: BarMenuItem) {
                customListBalloon.dismiss()
                when (customItem.index) {
                    1 -> {
                        findNavController().navigate(R.id.to_regist)
                    }
                   /* 2 -> {
                        findNavController().navigate(R.id.to_scan)
                    }*/
                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuBtn = view.findViewById<View>(R.id.right_icon).apply {
            setOnClickListener {
                customListBalloon.showAlignBottom(it)
            }
        }
        customListBalloon = Balloon.Builder(menuBtn.context).setLayout(R.layout.layout_custom_list).setArrowSize(12).setArrowOrientation(ArrowOrientation.TOP).setArrowPosition(0.85f).setTextSize(12f).setCornerRadius(4f).setBalloonAnimation(BalloonAnimation.CIRCULAR).setBackgroundColorResource(R.color.menu_bg).setBalloonAnimation(BalloonAnimation.FADE).setDismissWhenClicked(true).setOnBalloonClickListener(object : OnBalloonClickListener {
            override fun onBalloonClick(view: View) {
            }
        }).setDismissWhenClicked(true).setOnBalloonOutsideTouchListener(object : OnBalloonOutsideTouchListener {
            override fun onBalloonOutsideTouch(view: View, event: MotionEvent) {
                menuBtn.isClickable = false
                customListBalloon.dismiss()
                menuBtn.postDelayed({
                    menuBtn.isClickable = true
                }, 300)
            }
        }).build()

        customListBalloon.getContentView().findViewById<RecyclerView>(R.id.list_recyclerView).apply {
            setHasFixedSize(true)
            adapter = customAdapter
            customAdapter.addCustomItem(ItemUtils.getCustomSamples(requireContext()))
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
    }
}