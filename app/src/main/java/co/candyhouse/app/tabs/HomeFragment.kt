package co.candyhouse.app.tabs

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.RelativeLayout
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseNFG
import co.candyhouse.app.base.view.IBaseView
import co.candyhouse.app.tabs.menu.BarMenuItem
import co.candyhouse.app.tabs.menu.CustomAdapter
import co.candyhouse.app.tabs.menu.ItemUtils
import co.candyhouse.app.util.safeNavigate

abstract class HomeFragment<T : ViewBinding> : BaseNFG<T>(), IBaseView {
    private var customListPopup: PopupWindow? = null
    private val customAdapter by lazy {
        CustomAdapter(object : CustomAdapter.CustomViewHolder.Delegate {
            override fun onCustomItemClick(customItem: BarMenuItem) {
                customListPopup?.dismiss()
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

        initializeCustomListPopup(view)

        // 每次视图创建都需要执行
        setupUI()
        setupListeners()
        observeViewModelData(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initializeCustomListPopup(view: View) {
        val menuBtn = view.findViewById<View>(R.id.right_icon).apply {
            setOnClickListener {
                showCustomListPopup(it)
            }
        }

        val popupView = LayoutInflater.from(menuBtn.context).inflate(R.layout.layout_balloon, null)
        val arrowSize = (12 * resources.displayMetrics.density).toInt()
        popupView.findViewById<ImageView>(R.id.balloon_arrow).apply {
            layoutParams = RelativeLayout.LayoutParams(arrowSize, arrowSize).apply {
                addRule(RelativeLayout.ALIGN_TOP, R.id.balloon_content)
            }
            rotation = 0f
            ImageViewCompat.setImageTintList(
                this,
                ContextCompat.getColorStateList(context, R.color.menu_bg)
            )
        }
        popupView.findViewById<CardView>(R.id.balloon_card).apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.menu_bg))
            radius = 4 * resources.displayMetrics.density
        }
        popupView.findViewById<View>(R.id.balloon_content).setPadding(
            arrowSize - 2,
            arrowSize - 2,
            arrowSize - 2,
            arrowSize - 2
        )
        val content = popupView.findViewById<android.view.ViewGroup>(R.id.balloon_detail).apply {
            removeAllViews()
            setPadding(0, 0, 0, 0)
            LayoutInflater.from(context).inflate(R.layout.layout_custom_list, this, true)
        }
        content.findViewById<RecyclerView>(R.id.list_recyclerView)
            .apply {
                setHasFixedSize(true)
                adapter = customAdapter
                customAdapter.addCustomItem(ItemUtils.getCustomSamples(requireContext()))
                layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            }

        customListPopup = PopupWindow(
            popupView,
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            elevation = 2 * resources.displayMetrics.density
            animationStyle = R.style.Fade
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            isOutsideTouchable = true
            setTouchInterceptor { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    menuBtn.isClickable = false
                    dismiss()
                    menuBtn.postDelayed({ menuBtn.isClickable = true }, 300)
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun showCustomListPopup(anchor: View) {
        val popup = customListPopup ?: return
        if (popup.isShowing) {
            popup.dismiss()
            return
        }

        popup.contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        popup.width = popup.contentView.measuredWidth
        popup.height = popup.contentView.measuredHeight
        popup.contentView.findViewById<View>(R.id.balloon_arrow).apply {
            layoutParams = (layoutParams as RelativeLayout.LayoutParams).apply {
                leftMargin = (popup.width * 0.85f - measuredWidth / 2f).toInt()
            }
        }
        popup.showAsDropDown(anchor, anchor.measuredWidth / 2 - popup.width / 2, 0)
    }

    override fun onDestroyView() {
        customListPopup?.dismiss()
        customListPopup = null
        super.onDestroyView()
    }

}