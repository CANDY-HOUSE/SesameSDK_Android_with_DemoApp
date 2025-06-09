package candyhouse.sesameos.ir.fg

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.adapter.IrCompanyAdapter
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.databinding.FgIrCpfgBinding
import candyhouse.sesameos.ir.ext.Config
import candyhouse.sesameos.ir.ext.Ext
import candyhouse.sesameos.ir.ext.IRDeviceType
import co.candyhouse.sesame.utils.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList
import java.util.Locale


class IrCompanyFg : IrBaseFG<FgIrCpfgBinding>() {

    override fun getViewBinder() = FgIrCpfgBinding.inflate(layoutInflater)
    private val tag: String = IrCompanyFg::class.java.simpleName

    private var originalY: Float = 0f

    var searchList: MutableList<IrRemote> = mutableListOf()
    private lateinit var irCompanyAdapter: IrCompanyAdapter
    private lateinit var searchAdapter: IrCompanyAdapter
    private val controlList = mutableListOf<IrRemote>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCompanyRecyclerView()
        setupSearchView()
        setupSideBarView()
        setupSearchAnimation()
        initData()
    }

    private fun initData() {
        initIrRemoteData()
    }

    private fun setupSearchView() {
        searchAdapter = IrCompanyAdapter(requireActivity(), searchList) { irRemote,position ->
            gotoIrControlView(irRemote)
        }
        bind.editRyView.adapter = searchAdapter
        bind.edtTv.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.toString().isNotEmpty()) {
                    performSearch(s.toString())
                } else {
                    restoreInitialState()
                }
            }
        })
    }

    private fun gotoIrControlView(irRemoteDevice: IrRemote) {
        safeNavigate(R.id.action_to_irgridefg, Bundle().apply {
            this.putInt(Config.productKey, arguments?.getInt(Config.productKey) ?: -1)
            this.putParcelable(Config.irDevice, irRemoteDevice)
            this.putBoolean(Config.isNewDevice, true)
        }).also { clearSearchBar() }
    }

    fun performSearch(key: String) {
        if (key.isEmpty()) return
        searchList.clear()

        irCompanyAdapter.children.forEach { parentItem ->
            if (parentItem.model?.uppercase()?.contains(key.uppercase()) == true) {
                searchList.add(parentItem)
            }
        }
        restoreInitialState(true)
    }

    fun restoreInitialState(isShowEdit: Boolean = false) {
        if (isShowEdit) {
            bind.indexTb.visibility = View.GONE
            bind.editRyView.visibility = View.VISIBLE
            searchAdapter.updateChildren(searchList, bind.edtTv.text.toString())
        } else {
            bind.indexTb.visibility = View.VISIBLE
            bind.editRyView.visibility = View.GONE
            searchList.clear()
        }
    }

    private fun setupCompanyRecyclerView() {
        irCompanyAdapter = IrCompanyAdapter(requireActivity(), emptyList()) { irRemote,position  ->
            gotoIrControlView(irRemote)
        }
        bind.indexTb.adapter = irCompanyAdapter
    }

    private fun setupSideBarView() {
        bind.sideBar.setOnTouchingLetterChangedListener { k ->
            for (i in 0 until irCompanyAdapter.children.size) {
                if (irCompanyAdapter.children[i].direction?.uppercase(Locale.ROOT) == k.toUpperCase()) {
                    bind.indexTb.scrollToPosition(i)
                    break
                }
            }
        }
    }


    private fun showControlView() {
        irCompanyAdapter.updateChildren(controlList)
        CoroutineScope(Dispatchers.IO).launch {
            setupSideBar(controlList)
        }
    }

    private suspend fun setupSideBar(list: List<IrRemote>) {
        val direction = mutableListOf<String>()
        list.forEach {
            if (it.direction != null && !direction.contains(it.direction)) {
                direction.add(it.direction)
            }
        }
        withContext(Dispatchers.Main) {
            bind.sideBar.setDirection(ArrayList(direction))
        }
    }

    fun deviceKeyToAttrs(key: Int): Int {
        var value = R.raw.air_control_type
        if (key == IRDeviceType.DEVICE_REMOTE_AIR) {
            value = R.raw.air_control_type
        } else if (key == IRDeviceType.DEVICE_REMOTE_HW) {
            value = R.array.strs_hw_type
        } else if (key == IRDeviceType.DEVICE_REMOTE_AP) {
            value = R.array.strs_ap_type
        } else if (key == IRDeviceType.DEVICE_REMOTE_TV) {
            value = R.raw.tv_control_type
        } else if (key == IRDeviceType.DEVICE_REMOTE_IPTV) {
            value = R.array.strs_iptv_type
        } else if (key == IRDeviceType.DEVICE_REMOTE_BOX) {
            value = R.array.strs_stb_type
        } else if (key == IRDeviceType.DEVICE_REMOTE_DVD) {
            value = R.array.strs_dvd_type
        } else if (key == IRDeviceType.DEVICE_REMOTE_FANS) {
            value = R.array.strs_fans_type
        } else if (key == IRDeviceType.DEVICE_REMOTE_PJT) {
            value = R.array.strs_pjt_type
        } else if (key == IRDeviceType.DEVICE_REMOTE_LIGHT) {
//            value = R.array.strs_light_type
            value = R.raw.light_control_type
        } else if (key == IRDeviceType.DEVICE_REMOTE_DC) {
            value = R.array.strs_dc_type
        } else if (key == IRDeviceType.DEVICE_REMOTE_AUDIO) {
            value = R.array.strs_audio_type
        } else if (key == IRDeviceType.DEVICE_REMOTE_ROBOT) {
            value = R.array.strs_robot_type
        }
        return value
    }

    private fun initIrRemoteData() {
        CoroutineScope(Dispatchers.IO).launch {
            val key = arguments?.getInt(Config.productKey) ?: -1
            controlList.addAll(Ext.parseJsonToDeviceList(requireContext(), deviceKeyToAttrs(key),key))
            withContext(Dispatchers.Main) {
                showControlView()
            }
        }
    }

    private fun setupSearchAnimation() {
        // 保存原始位置
        bind.linearLayoutResearch.post {
            originalY = bind.linearLayoutResearch.translationY
        }

        // 编辑框焦点监听
        bind.edtTv.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                expandSearchBar()
            }
        }

        // 取消按钮点击监听
        bind.textViewSearchCancel.setOnClickListener {
            collapseSearchBar()
        }
    }

    private fun expandSearchBar() {
        val animSet = AnimatorSet()

        // 使用translationY代替y
        val moveUpAnim = ObjectAnimator.ofFloat(
            bind.linearLayoutResearch,
            "translationY",
            bind.linearLayoutResearch.translationY,
            0f
        )
        moveUpAnim.duration = 800

        val fadeOutTopTitle = ObjectAnimator.ofFloat(bind.linearLayoutTitle, "alpha", 1f, 0f)
        val fadeOutNotice = ObjectAnimator.ofFloat(bind.textviewIrCompanyNotice, "alpha", 1f, 0f)
        val fadeInCancel = ObjectAnimator.ofFloat(bind.textViewSearchCancel, "alpha", 0f, 1f)

        fadeOutTopTitle.duration = 800
        fadeOutNotice.duration = 800
        fadeInCancel.duration = 800

        animSet.start()

        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                bind.textViewSearchCancel.visibility = View.VISIBLE
                updateEditTextMargin(false)
                bind.textViewSearchCancel.alpha = 1f
            }

            override fun onAnimationEnd(animation: Animator) {
                bind.linearLayoutTitle.visibility = View.GONE
                bind.textviewIrCompanyNotice.visibility = View.GONE
            }
        })

        animSet.start()
    }

    private fun collapseSearchBar() {
        val animSet = AnimatorSet()
        val moveDownAnim = ObjectAnimator.ofFloat(
            bind.linearLayoutResearch,
            "translationY",
            bind.linearLayoutResearch.translationY,
            originalY
        )
        moveDownAnim.duration = 900

        // 其他动画保持不变
        val fadeInTopTitle = ObjectAnimator.ofFloat(bind.linearLayoutTitle, "alpha", 0f, 1f)
        val fadeInNotice = ObjectAnimator.ofFloat(bind.textviewIrCompanyNotice, "alpha", 0f, 1f)

        fadeInTopTitle.duration = 800
        fadeInNotice.duration = 800

        animSet.playTogether(moveDownAnim, fadeInTopTitle, fadeInNotice)

        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                bind.edtTv.text.clear()
                bind.textViewSearchCancel.visibility = View.GONE
                bind.linearLayoutTitle.visibility = View.VISIBLE
                bind.textviewIrCompanyNotice.visibility = View.VISIBLE
                updateEditTextMargin(true)
            }

            override fun onAnimationEnd(animation: Animator) {
                bind.edtTv.clearFocus()
                hideKeyboard()
            }
        })

        animSet.start()
    }

    private fun clearSearchBar() {
        bind.edtTv.text.clear()
        bind.textViewSearchCancel.visibility = View.GONE
        bind.linearLayoutTitle.visibility = View.VISIBLE
        bind.textviewIrCompanyNotice.visibility = View.VISIBLE
        updateEditTextMargin(true)
        bind.edtTv.clearFocus()
        hideKeyboard()
    }

    private fun updateEditTextMargin(showMargin: Boolean) {
        val params = bind.edtTv.layoutParams as LinearLayout.LayoutParams
        params.marginEnd = if (showMargin) {
            resources.getDimensionPixelSize(R.dimen.margin_end_collapsed)
        } else {
            0
        }
        bind.edtTv.layoutParams = params
    }

    // 隐藏键盘的辅助方法
    private fun hideKeyboard() {
        //增加：Fragment 是否已经附加到 Activity（来自firebase crash）
        if (isAdded) {
            val imm =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(bind.edtTv.windowToken, 0)
        }
    }


}
