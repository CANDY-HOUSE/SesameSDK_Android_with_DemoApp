package co.candyhouse.app.tabs

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseNFG
import co.candyhouse.app.base.view.IBaseView
import co.candyhouse.app.tabs.menu.BarMenuItem
import co.candyhouse.app.tabs.menu.CustomAdapter
import co.candyhouse.app.tabs.menu.ItemUtils
import co.utils.safeNavigate
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.OnBalloonClickListener
import com.skydoves.balloon.OnBalloonOutsideTouchListener

abstract class HomeFragment<T : ViewBinding> : BaseNFG<T>(), IBaseView {

    lateinit var customListBalloon: Balloon
    private val customAdapter by lazy {
        CustomAdapter(object : CustomAdapter.CustomViewHolder.Delegate {
            override fun onCustomItemClick(customItem: BarMenuItem) {
                customListBalloon.dismiss()
                when (customItem.index) {
                    1 -> {
                        safeNavigate(R.id.to_regist)
                    }

                    2 -> {
                        safeNavigate(R.id.to_scan)
                    }

                    3 -> {
                        safeNavigate(R.id.action_to_webViewFragment, Bundle().apply {
                            putString("scene", "contact-add")
                        })
                    }
                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeCustomListBalloon(view)

        // 每次视图创建都需要执行
        setupUI()
        setupListeners()
        observeViewModelData(view)
    }

    private fun initializeCustomListBalloon(view: View) {
        val menuBtn = view.findViewById<View>(R.id.right_icon).apply {
            setOnClickListener {
                customListBalloon.showAlignBottom(it)
            }
        }
        customListBalloon =
            Balloon.Builder(menuBtn.context).setLayout(R.layout.layout_custom_list).setArrowSize(12)
                .setArrowOrientation(ArrowOrientation.TOP).setArrowPosition(0.85f).setTextSize(12f)
                .setCornerRadius(4f).setBalloonAnimation(BalloonAnimation.CIRCULAR)
                .setBackgroundColorResource(R.color.menu_bg)
                .setBalloonAnimation(BalloonAnimation.FADE).setDismissWhenClicked(true)
                .setOnBalloonClickListener(object : OnBalloonClickListener {
                    override fun onBalloonClick(view: View) {
                    }
                }).setDismissWhenClicked(true)
                .setOnBalloonOutsideTouchListener(object : OnBalloonOutsideTouchListener {
                    override fun onBalloonOutsideTouch(view: View, event: MotionEvent) {
                        menuBtn.isClickable = false
                        customListBalloon.dismiss()
                        menuBtn.postDelayed({
                            menuBtn.isClickable = true
                        }, 300)
                    }
                }).build()

        customListBalloon.getContentView().findViewById<RecyclerView>(R.id.list_recyclerView)
            .apply {
                setHasFixedSize(true)
                adapter = customAdapter
                customAdapter.addCustomItem(ItemUtils.getCustomSamples(requireContext()))
                layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            }
    }

}