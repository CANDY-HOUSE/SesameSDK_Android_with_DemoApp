package candyhouse.sesameos.ir.fg

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.adapter.IrCompanyAdapter
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.databinding.FgIrCpfgBinding
import candyhouse.sesameos.ir.ext.Config
import candyhouse.sesameos.ir.ext.Ext
import candyhouse.sesameos.ir.ext.IRDeviceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.Locale

class IrCompanyFg : IrBaseFG<FgIrCpfgBinding>() {

    override fun getViewBinder() = FgIrCpfgBinding.inflate(layoutInflater)
    private val tag: String = IrCompanyFg::class.java.simpleName

    private var originalY: Float = 0f
    private var isInSearchMode = false
    private val masterList = Collections.synchronizedList(mutableListOf<IrRemote>())
    private var currentDisplayList: List<IrRemote> = emptyList()
    private lateinit var irAdapter: IrCompanyAdapter
    private var searchJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearchView()
        setupSideBarView()
        setupSearchAnimation()
        initData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        searchJob = null
    }

    private fun initData() {
        initIrRemoteData()
    }

    private fun setupRecyclerView() {
        irAdapter = IrCompanyAdapter(requireActivity()) { irRemote, position ->
            gotoIrControlView(irRemote)
        }
        bind.indexTb.adapter = irAdapter
    }

    private fun setupSearchView() {
        bind.edtTv.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val searchText = s?.toString() ?: ""
                if (searchText.isNotEmpty()) {
                    performSearch(searchText)
                } else if (isInSearchMode) {
                    exitSearchMode()
                }
            }
        })
    }

    private fun gotoIrControlView(irRemoteDevice: IrRemote) {
        updateRemoteDevice(irRemoteDevice)
        safeNavigate(R.id.action_to_irgridefg, Bundle().apply {
            this.putInt(Config.productKey, arguments?.getInt(Config.productKey) ?: -1)
            this.putParcelable(Config.irDevice, irRemoteDevice)
            this.putBoolean(Config.isNewDevice, true)
        })
    }

    private fun updateRemoteDevice(irRemoteDevice: IrRemote) {
        // 更新当前的IRRemoteDevice
        if (irRemoteDevice.alias.contains("\n")) {
            irRemoteDevice.alias = irRemoteDevice.alias.substringBefore("\n").trim()
        }
    }

    private fun performSearch(key: String) {
        if (key.isEmpty()) return
        searchJob?.cancel()

        if (!isInSearchMode) {
            enterSearchMode()
        }
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            val filteredList = withContext(Dispatchers.Default) {
                synchronized(masterList) {
                    masterList.filter { it.alias.uppercase().contains(key.uppercase()) }
                }
            }
            currentDisplayList = filteredList
            irAdapter.updateData(filteredList, key, true)
        }
    }

    private fun enterSearchMode() {
        isInSearchMode = true
        expandSearchBar()
        bind.sideBar.visibility = View.GONE
    }

    private fun exitSearchMode() {
        isInSearchMode = false
        collapseSearchBar()
        currentDisplayList = masterList
        irAdapter.updateData(masterList, "", false)
        bind.sideBar.visibility = View.VISIBLE
    }

    private fun setupSideBarView() {
        bind.sideBar.setOnTouchingLetterChangedListener { k ->
            if (!isInSearchMode) {
                for (i in 0 until masterList.size) {
                    if (masterList[i].direction?.uppercase(Locale.ROOT) == k.toUpperCase()) {
                        bind.indexTb.scrollToPosition(i)
                        break
                    }
                }
            }
        }
    }

    private fun showControlView() {
        currentDisplayList = masterList
        irAdapter.updateData(masterList)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            setupSideBar(masterList)
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
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            if (masterList.isEmpty()) {
                synchronized(masterList) {
                    val key = arguments?.getInt(Config.productKey) ?: -1
                    masterList.addAll(Ext.parseJsonToDeviceList(requireContext(), deviceKeyToAttrs(key), key))
                }
            }
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
            if (hasFocus && !isInSearchMode) {
                enterSearchMode()
            }
        }

        // 取消按钮点击监听
        bind.textViewSearchCancel.setOnClickListener {
            exitSearchMode()
        }
    }

    private fun expandSearchBar() {
        bind.indexTb.stopScroll()
        val animSet = AnimatorSet()
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

        animSet.playTogether(moveUpAnim, fadeOutTopTitle, fadeOutNotice, fadeInCancel)
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                bind.textViewSearchCancel.visibility = View.VISIBLE
                updateEditTextMargin(false)
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
        val fadeOutCancel = ObjectAnimator.ofFloat(bind.textViewSearchCancel, "alpha", 1f, 0f)

        fadeInTopTitle.duration = 800
        fadeInNotice.duration = 800
        fadeOutCancel.duration = 800

        animSet.playTogether(moveDownAnim, fadeInTopTitle, fadeInNotice, fadeOutCancel)

        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                bind.edtTv.text.clear()
                bind.linearLayoutTitle.visibility = View.VISIBLE
                bind.textviewIrCompanyNotice.visibility = View.VISIBLE
                updateEditTextMargin(true)
            }

            override fun onAnimationEnd(animation: Animator) {
                bind.textViewSearchCancel.visibility = View.GONE
                bind.edtTv.clearFocus()
                hideKeyboard()
            }
        })
        animSet.start()
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
