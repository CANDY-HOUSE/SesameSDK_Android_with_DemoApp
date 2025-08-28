package co.candyhouse.app.tabs.devices

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.IrRemote
import co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.RemoteBundleKeyConfig
import co.candyhouse.app.R
import co.candyhouse.app.databinding.FgDevicelistBinding
import co.candyhouse.app.tabs.HomeFragment
import co.candyhouse.app.tabs.devices.hub3.setting.ir.remoteControl.domain.bizAdapter.bizBase.IRType
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.utils.L
import co.utils.getParcelableCompat
import co.utils.recycle.DeviceListAdapter
import co.utils.recycle.GenericAdapter
import co.utils.recycle.SimpleItemTouchHelperCallback
import co.utils.safeNavigate
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DeviceListFG : HomeFragment<FgDevicelistBinding>() {
    private val tag = "DeviceListFG"

    override fun getViewBinder() = FgDevicelistBinding.inflate(layoutInflater)

    private lateinit var adapter: GenericAdapter<CHDevices>
    private lateinit var itemTouchHelper: ItemTouchHelper

    private var isSearchVisible = true
    private var searchJob: Job? = null
    private var lastSearchQuery = ""

    override fun onResume() {
        super.onResume()
        bind.leaderboardList?.let { recyclerView ->
            itemTouchHelper = ItemTouchHelper(SimpleItemTouchHelperCallback(adapter)).apply {
                attachToRecyclerView(recyclerView)
            }
        }
    }

    override fun onPause() {
        try {
            if (::itemTouchHelper.isInitialized) {
                // 在分离前确保 RecyclerView 还在有效状态
                if (bind.leaderboardList?.isAttachedToWindow == true) {
                    itemTouchHelper.attachToRecyclerView(null)
                }
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().apply {
                log("Error in onPause detaching ItemTouchHelper")
                recordException(e)
            }
        }

        bind.searchEditText.text.clear()
        super.onPause()
    }

    override fun setupUI() {
        initializeAdapter()

        bind.appBarLayout.setExpanded(false, false)
        bind.leaderboardList.setEmptyView(bind.emptyView)
        bind.leaderboardList.adapter = adapter
    }

    override fun setupListeners() {
        //下拉刷新
        bind.swiperefresh.setOnRefreshListener {
            L.d(tag, "下拉刷新...")
            bind.swiperefresh.isRefreshing = true
            CHDeviceManager.isRefresh.set(true)
            mDeviceViewModel.refleshDevices()
        }

        setupSearchBehavior()
        setupSearchEditTextListener()
        setupBackViewListener()
    }

    private fun setupSearchBehavior() {
        bind.appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            val scrollPercentage = Math.abs(verticalOffset).toFloat() / totalScrollRange

            val wasVisible = isSearchVisible
            isSearchVisible = scrollPercentage < 0.5f

            if (wasVisible && !isSearchVisible) {
                hideKeyboard()
            }

            bind.searchEditText.alpha = 1 - scrollPercentage
        }

        bind.leaderboardList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                // 向上滚动且键盘显示时，收起键盘
                if (dy > 0 && isKeyboardVisible()) {
                    hideKeyboard()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    CHDeviceManager.isScroll.set(false)
                } else {
                    CHDeviceManager.isScroll.set(true)
                }
            }
        })
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(bind.searchEditText.windowToken, 0)
        bind.searchEditText.clearFocus()
    }

    private fun isKeyboardVisible(): Boolean {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.isAcceptingText
    }

    private fun setupSearchEditTextListener() {
        bind.searchEditText.doOnTextChanged { text, _, _, _ ->
            val currentQuery = text.toString()

            if (currentQuery == lastSearchQuery) {
                return@doOnTextChanged
            }

            lastSearchQuery = currentQuery
            searchJob?.cancel()

            // 延迟 300ms 执行搜索，避免频繁触发
            searchJob = lifecycleScope.launch {
                delay(300)
                performSearch(currentQuery)
            }
        }

        bind.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                true
            } else {
                false
            }
        }
    }

    private fun performSearch(query: String) {
        mDeviceViewModel.updateSearchQuery(query)
    }

    override fun <T : View> observeViewModelData(view: T) {
        // 初次加载列表
        lifecycleScope.launch {
            mDeviceViewModel.myChDevices.collect {
                view.post {
                    checkAdapterPost {
                        CHDeviceManager.isRefresh.set(false)
                        bind.swiperefresh.isRefreshing = false
                        adapter.updateList(mDeviceViewModel.myChDevices.value)
                    }
                }
            }
        }
        // 刷新列表
        mDeviceViewModel.neeReflesh.observe(viewLifecycleOwner) { isR ->
            CHDeviceManager.isRefresh.set(false)
            bind.swiperefresh.isRefreshing = false

            L.d(tag, "neeReflesh... ${isR.postion}")
            val displayList = if (mDeviceViewModel.searchQuery.value.isEmpty()) {
                mDeviceViewModel.myChDevices.value
            } else {
                // 重新过滤，确保获取最新状态
                val query = mDeviceViewModel.searchQuery.value
                L.d(tag, "刷新搜索结果页面... $query")
                ArrayList(mDeviceViewModel.myChDevices.value.filter { device ->
                    device.getNickname().contains(query, ignoreCase = true) ||
                            device.deviceId?.toString()?.contains(query, ignoreCase = true) == true
                })
            }

            adapter.updateList(displayList, isR.postion)
        }
        // 监听过滤后的列表
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mDeviceViewModel.filteredDevices.collect { devices ->
                    L.d(tag, "监听过滤后的列表数目  ${devices.size}")
                    adapter.updateList(ArrayList(devices))
                }
            }
        }
    }

    private fun initializeAdapter() {
        adapter = DeviceListAdapter(
            mDeviceViewModel,
            onDeviceClick = { device ->
                // 处理列表item点击
                dispatchOnDeviceClick(device)
            },
            callBackHub3 = { hub, irRemote ->
                // 处理Hub3红外设备点击
                handleCallBackHub3(hub, irRemote)
            }
        )
    }

    private fun handleCallBackHub3(hub3: CHHub3, irRemote: IrRemote) {
        L.d(tag, "点击item：" + irRemote.alias + " " + irRemote.type + " " + irRemote.code)
        when (irRemote.type) {
            IRType.DEVICE_REMOTE_CUSTOM -> {
                //学习
                mDeviceViewModel.ssmLockLiveData.value = hub3
                val bundle = Bundle().apply {
                    putParcelable(RemoteBundleKeyConfig.irDevice, irRemote)
                    putBoolean(RemoteBundleKeyConfig.editable, false)
                }
                safeNavigate(R.id.remoteLearnFg, bundle)
            }

            IRType.DEVICE_REMOTE_AIR, IRType.DEVICE_REMOTE_LIGHT, IRType.DEVICE_REMOTE_TV -> {
                //空调
                mDeviceViewModel.ssmLockLiveData.value = hub3
                safeNavigate(R.id.action_to_irgridefg2, Bundle().apply {
                    this.putParcelable(RemoteBundleKeyConfig.irDevice, irRemote)
                    this.putBoolean(RemoteBundleKeyConfig.isNewDevice, false)
                    putBoolean(RemoteBundleKeyConfig.editable, false)
                })
            }

            else -> {
                L.d(tag, "暂不支持未知类型跳转...")
            }
        }
    }

    private fun checkAdapterPost(call: () -> Unit) {
        if (!bind.leaderboardList.isComputingLayout && !bind.leaderboardList.isLayoutFrozen) {
            call.invoke()
        } else {
            bind.leaderboardList.post {
                call.invoke()
            }
        }
    }

    private fun dispatchOnDeviceClick(device: CHDevices) {
        mDeviceViewModel.ssmLockLiveData.value = device
        when (device.productModel) {
            CHProductModel.WM2 -> safeNavigate(R.id.to_WM2SettingFG)
            CHProductModel.SS2, CHProductModel.SS4 -> safeNavigate(if (device.getLevel() == 2) R.id.action_deviceListPG_to_SSM2SettingFG else R.id.action_deviceListPG_to_mainRoomFG)
            CHProductModel.SesameBot1 -> safeNavigate(R.id.action_deviceListPG_to_SesameBotSettingFG)
            CHProductModel.BiKeLock -> safeNavigate(R.id.action_deviceListPG_to_sesameBikeSettingFG)
            CHProductModel.BiKeLock2 -> safeNavigate(R.id.action_deviceListPG_to_sesameBikeSettingFG)
            CHProductModel.SS5, CHProductModel.SS5PRO, CHProductModel.SS5US, CHProductModel.SS6Pro, CHProductModel.BLEConnector -> safeNavigate(
                if (device.getLevel() == 2) R.id.to_Sesame5SettingFG else R.id.action_deviceListPG_to_mainRoomSS5FG
            )

            CHProductModel.SSMOpenSensor -> safeNavigate(R.id.to_SesameOpenSensorSettingFG)
            CHProductModel.SSMTouchPro -> safeNavigate(R.id.to_SesameTouchProSettingFG)
            CHProductModel.SSMTouch -> safeNavigate(R.id.to_SesameTouchProSettingFG)
            CHProductModel.Hub3 -> safeNavigate(R.id.to_Hub3SettingFG)
            CHProductModel.Remote -> safeNavigate(R.id.to_SesameTouchProSettingFG)
            CHProductModel.RemoteNano -> safeNavigate(R.id.to_SesameOpenSensorSettingFG)
            CHProductModel.SesameBot2 -> safeNavigate(R.id.to_SesameBot2SettingFG)
            CHProductModel.SSMFace -> safeNavigate(R.id.to_SesameTouchProSettingFG)
            CHProductModel.SSMFacePro -> safeNavigate(R.id.to_SesameTouchProSettingFG)
            CHProductModel.SSMFaceProAI -> safeNavigate(R.id.to_SesameTouchProSettingFG)
            CHProductModel.SSMFaceAI -> safeNavigate(R.id.to_SesameTouchProSettingFG)
        }
    }

    private fun setupBackViewListener() {
        setFragmentResultListener(RemoteBundleKeyConfig.controlIrDeviceResult) { _, bundle ->
            var irRemote: IrRemote? = null
            var chDeviceId: String? = null
            if (bundle.containsKey(RemoteBundleKeyConfig.irDevice)) {
                irRemote = bundle.getParcelableCompat<IrRemote>(RemoteBundleKeyConfig.irDevice)
            }
            if (bundle.containsKey(RemoteBundleKeyConfig.chDeviceId)) {
                chDeviceId = bundle.getString(RemoteBundleKeyConfig.chDeviceId)
            }
            if (irRemote != null && !chDeviceId.isNullOrEmpty()) {
                mDeviceViewModel.updateHub3IrDevice(irRemote, chDeviceId)
            }
        }
    }

}
