package co.candyhouse.app.tabs.devices.hub3.setting.ir.remotes

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
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remotes.cache.IrRemoteCacheManager
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.RemoteBundleKeyConfig
import co.candyhouse.server.CHIRAPIManager
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseNFG
import co.candyhouse.app.databinding.FgRemoteListBinding
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.utils.L
import co.utils.safeNavigate
import co.utils.safeNavigateBack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.Locale
import java.util.UUID
import kotlin.collections.isNullOrEmpty

class RemoteListFg : BaseNFG<FgRemoteListBinding>() {

    override fun getViewBinder() = FgRemoteListBinding.inflate(layoutInflater)
    private val tag: String = RemoteListFg::class.java.simpleName

    private var originalY: Float = 0f
    private var isInSearchMode = false
    private val masterList = Collections.synchronizedList(mutableListOf<IrRemote>())
    private var currentDisplayList: List<IrRemote> = emptyList()
    private lateinit var irAdapter: RemoteListAdapter
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupIRRemoteCache()
        if (mDeviceViewModel.ssmLockLiveData.value == null || mDeviceViewModel.ssmLockLiveData.value !is CHHub3) {
            safeNavigateBack()
        }
    }

    private fun setupIRRemoteCache() {
        IrRemoteCacheManager.init(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearchView()
        setupSideBarView()
        setupSearchAnimation()
        setupRetryView()
        if (masterList.isEmpty()) {
            getRemoteList()
        } else {
            showContentView()
            showControlView()
        }
    }

    private fun setupRetryView() {
        bind.errorLayout.setOnClickListener {
            getRemoteList()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        searchJob = null
    }

    private fun getRemoteList() {
        if (masterList.isEmpty()){
            showLoadingView()
        }

        arguments?.getInt(RemoteBundleKeyConfig.productKey) ?.let { brandType ->
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val cachedList = IrRemoteCacheManager.getValidCache(brandType)
                if (!cachedList.isNullOrEmpty()) {
                    synchronized(masterList) {
                        masterList.clear()
                        masterList.addAll(cachedList)
                        view?.post {
                            showContentView()
                            showControlView()
                        }
                    }
                }
                fetchRemoteListFromServer(brandType)
            }
        }
    }

    private fun fetchRemoteListFromServer(brandType: Int) {
        CHIRAPIManager.fetchRemoteList(brandType) {
            it.onSuccess {
                val remoteList = swapRemoteList(brandType,it.data)
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    IrRemoteCacheManager.saveCache(brandType, remoteList)
                }
                synchronized(masterList) {
                    masterList.clear()
                    masterList.addAll(remoteList)
                    view?.post {
                        showContentView()
                        showControlView()
                    }
                }
            }
            it.onFailure { error ->
                // 处理错误情况
                L.e(tag, "Error fetching remote list: ${error.message}")
                view?.post {
                    if (masterList.size == 0){
                        showErrorView()
                    }
                }

            }
        }
    }

    private fun showLoadingView() {
        bind.errorLayout.visibility = View.GONE
        bind.progressBar.visibility = View.VISIBLE
        bind.indexTb.visibility = View.GONE
        bind.textviewIrBrandNotice.visibility = View.GONE
        bind.linearLayoutResearch.visibility = View.GONE
        bind.sideBar.visibility = View.GONE
    }

    private fun showErrorView() {
        bind.errorLayout.visibility = View.VISIBLE
        bind.progressBar.visibility = View.GONE
        bind.indexTb.visibility = View.GONE
        bind.textviewIrBrandNotice.visibility = View.GONE
        bind.linearLayoutResearch.visibility = View.GONE
        bind.sideBar.visibility = View.GONE
    }

    private fun showContentView() {
        bind.errorLayout.visibility = View.GONE
        bind.progressBar.visibility = View.GONE
        bind.indexTb.visibility = View.VISIBLE
        bind.textviewIrBrandNotice.visibility = View.VISIBLE
        bind.linearLayoutResearch.visibility = View.VISIBLE
        bind.sideBar.visibility = View.VISIBLE
    }

    private fun swapRemoteList(brandType: Int, originList: List<IrRemote>): List<IrRemote> {
        return originList.map {
            it.copy(
                uuid = UUID.randomUUID().toString().uppercase(),
                timestamp = 0L,
                type = brandType,
            )
        }
    }

    private fun setupRecyclerView() {
        irAdapter = RemoteListAdapter(requireActivity()) { irRemote, position ->
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
        safeNavigate(R.id.action_to_remoteControlFg, Bundle().apply {
            this.putInt(RemoteBundleKeyConfig.productKey, arguments?.getInt(RemoteBundleKeyConfig.productKey) ?: -1)
            this.putParcelable(RemoteBundleKeyConfig.irDevice, irRemoteDevice)
            this.putBoolean(RemoteBundleKeyConfig.isNewDevice, true)
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
            it.direction?.let {
                if (!direction.contains(it)) {
                    direction.add(it)
                }
            }
        }
        withContext(Dispatchers.Main) {
            bind.sideBar.setDirection(ArrayList(direction))
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
        val fadeOutNotice = ObjectAnimator.ofFloat(bind.textviewIrBrandNotice, "alpha", 1f, 0f)
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
                bind.textviewIrBrandNotice.visibility = View.GONE
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
        val fadeInNotice = ObjectAnimator.ofFloat(bind.textviewIrBrandNotice, "alpha", 0f, 1f)
        val fadeOutCancel = ObjectAnimator.ofFloat(bind.textViewSearchCancel, "alpha", 1f, 0f)

        fadeInTopTitle.duration = 800
        fadeInNotice.duration = 800
        fadeOutCancel.duration = 800

        animSet.playTogether(moveDownAnim, fadeInTopTitle, fadeInNotice, fadeOutCancel)

        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                bind.edtTv.text.clear()
                bind.linearLayoutTitle.visibility = View.VISIBLE
                bind.textviewIrBrandNotice.visibility = View.VISIBLE
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