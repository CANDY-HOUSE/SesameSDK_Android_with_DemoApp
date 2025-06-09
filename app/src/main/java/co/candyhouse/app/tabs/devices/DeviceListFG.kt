package co.candyhouse.app.tabs.devices

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import candyhouse.sesameos.ir.base.BaseIr.Companion.hub3Device
import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.IRType
import candyhouse.sesameos.ir.ext.Config
import candyhouse.sesameos.ir.ext.Ext.getParcelableCompat
import co.candyhouse.app.R
import co.candyhouse.app.databinding.FgDevicelistBinding
import co.candyhouse.app.tabs.HomeFragment
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHHub3
import co.candyhouse.sesame.open.device.CHProductModel
import co.candyhouse.sesame.utils.L
import co.utils.recycle.DeviceListAdapter
import co.utils.recycle.GenericAdapter
import co.utils.recycle.SimpleItemTouchHelperCallback
import co.utils.safeNavigate
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.launch

class DeviceListFG : HomeFragment<FgDevicelistBinding>() {
    override fun getViewBinder() = FgDevicelistBinding.inflate(layoutInflater)

    private lateinit var adapter: GenericAdapter<CHDevices>
    private lateinit var itemTouchHelper: ItemTouchHelper

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

        super.onPause()
    }

    override fun setupUI() {
        initializeAdapter()

        bind.leaderboardList.setEmptyView(bind.emptyView)
        bind.leaderboardList.adapter = adapter
    }

    override fun setupListeners() {
        //下拉刷新
        bind.swiperefresh.setOnRefreshListener {
            bind.swiperefresh.isRefreshing = true
            CHDeviceManager.isRefresh.set(true)
            mDeviceViewModel.refleshDevices()
        }
        bind.leaderboardList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    CHDeviceManager.isScroll.set(false)
                } else {
                    CHDeviceManager.isScroll.set(true)
                }
            }
        })

        setupBackViewListener()
    }

    override fun <T : View> observeViewModelData(view: T) {
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
        mDeviceViewModel.neeReflesh.observe(viewLifecycleOwner) { isR ->
            CHDeviceManager.isRefresh.set(false)
            bind.swiperefresh.isRefreshing = false
            adapter.updateList(mDeviceViewModel.myChDevices.value, isR.postion)
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
        L.d("sf", "点击item：" + irRemote.alias + " " + irRemote.type + " " + irRemote.code)
        hub3Device.value = hub3
        //增加hub3Device.value空判断，如果为空提示用户下拉刷新
        if (hub3Device.value == null) {
            Toast.makeText(
                context,
                "Unexpected error. Pull to refresh.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        when (irRemote.type) {
            IRType.DEVICE_REMOTE_CUSTOM -> {
                //学习
                val bundle = Bundle().apply {
                    putParcelable(Config.irDevice, irRemote)
                    putBoolean(Config.editable, false)
                }
                safeNavigate(R.id.action_to_irdiy2, bundle)
            }

            IRType.DEVICE_REMOTE_AIR, IRType.DEVICE_REMOTE_LIGHT, IRType.DEVICE_REMOTE_TV -> {
                //空调
                safeNavigate(R.id.action_to_irgridefg2, Bundle().apply {
                    this.putParcelable(Config.irDevice, irRemote)
                    this.putBoolean(Config.isNewDevice, false)
                    putBoolean(Config.editable, false)
                })
            }

            else -> {
                L.d("sf", "暂不支持未知类型跳转...")
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
            CHProductModel.SS5, CHProductModel.SS5PRO, CHProductModel.SS5US, CHProductModel.SS6Pro -> safeNavigate(
                if (device.getLevel() == 2) R.id.to_Sesame5SettingFG else R.id.action_deviceListPG_to_mainRoomSS5FG
            )

            CHProductModel.SSMOpenSensor -> safeNavigate(R.id.to_SesameOpenSensorSettingFG)
            CHProductModel.SSMTouchPro -> safeNavigate(R.id.to_SesameTouchProSettingFG)
            CHProductModel.SSMTouch -> safeNavigate(R.id.to_SesameTouchProSettingFG)
            CHProductModel.BLEConnector -> safeNavigate(R.id.to_SesameTouchProSettingFG)
            CHProductModel.Hub3 -> safeNavigate(R.id.to_Hub3SettingFG)
            CHProductModel.Remote -> safeNavigate(R.id.to_SesameTouchProSettingFG)
            CHProductModel.RemoteNano -> safeNavigate(R.id.to_SesameOpenSensorSettingFG)
            CHProductModel.SesameBot2 -> safeNavigate(R.id.to_SesameBot2SettingFG)
            CHProductModel.SSMFace -> safeNavigate(R.id.to_SesameTouchProSettingFG)
            CHProductModel.SSMFacePro -> safeNavigate(R.id.to_SesameTouchProSettingFG)
            CHProductModel.SSMFaceProAI -> safeNavigate(R.id.to_SesameTouchProSettingFG)
        }
    }

    private fun setupBackViewListener() {
        setFragmentResultListener(Config.controlIrDeviceResult) { _, bundle ->
            var irRemote: IrRemote? = null
            var chDeviceId: String? = null
            if (bundle.containsKey(Config.irDevice)) {
                irRemote = bundle.getParcelableCompat<IrRemote>(Config.irDevice)
            }
            if (bundle.containsKey(Config.chDeviceId)) {
                chDeviceId = bundle.getString(Config.chDeviceId)
            }
            if (irRemote != null && !chDeviceId.isNullOrEmpty()) {
                mDeviceViewModel.updateHub3IrDevice(irRemote, chDeviceId)
            }
        }
    }

}
